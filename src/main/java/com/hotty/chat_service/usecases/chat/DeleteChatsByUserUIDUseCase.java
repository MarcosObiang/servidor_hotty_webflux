package com.hotty.chat_service.usecases.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hotty.chat_service.interfaces.ChatRepository;
import com.hotty.chat_service.model.ChatModel;
import com.hotty.common.services.EventPublisher;
import com.hotty.common.enums.PublishEventType;
import com.hotty.chat_service.usecases.messages.DeleteMessagesUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



/**
 * Caso de uso para eliminar los chats de un usuario específico por su UID.
 * Incluye eliminación en cascada de todos los mensajes asociados a cada chat.
 * Utiliza el repositorio ChatRepository para acceder a la base de datos.
 */

@Service
public class DeleteChatsByUserUIDUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteChatsByUserUIDUseCase.class);
    
    private final ChatRepository chatRepository;
    private final EventPublisher publisher;
    private final DeleteMessagesUseCase deleteMessagesUseCase;

    public DeleteChatsByUserUIDUseCase(
            ChatRepository chatRepository, 
            EventPublisher publisher,
            DeleteMessagesUseCase deleteMessagesUseCase) {
        this.chatRepository = chatRepository;
        this.publisher = publisher;
        this.deleteMessagesUseCase = deleteMessagesUseCase;
    }

    /**
     * Elimina todos los chats de un usuario y sus mensajes asociados.
     * 
     * @param userUID El UID del usuario cuyos chats se van a eliminar
     * @return Un Mono<Void> que se completa cuando la operación termina
     */
    public Mono<Void> execute(String userUID) {
        if (userUID == null || userUID.trim().isEmpty()) {
            log.warn("Attempted to delete chats with null or blank userUID");
            return Mono.error(new IllegalArgumentException("UserUID cannot be null or blank"));
        }

        return deleteMessagesAndChatsForUser(userUID)
                .flatMap(this::publishChatDeletionEvents)
                .then()
                .doOnSuccess(v -> 
                        log.info("Successfully deleted all chats and messages for user '{}'", userUID))
                .doOnError(error -> 
                        log.error("Failed to delete chats for user '{}': {}", userUID, error.getMessage(), error));
    }

    /**
     * Elimina primero todos los mensajes de cada chat del usuario, luego elimina los chats.
     */
    private Flux<ChatModel> deleteMessagesAndChatsForUser(String userUID) {
        // Primero obtenemos todos los chats del usuario
        return chatRepository.findByUserId(userUID)
                .flatMap(chat -> 
                    // Para cada chat, eliminamos sus mensajes primero
                    deleteMessagesUseCase.execute(chat.getChatId())
                            .thenReturn(chat)
                )
                .collectList()
                .flatMapMany(chatsWithMessagesDeleted -> {
                    if (chatsWithMessagesDeleted.isEmpty()) {
                        return Flux.empty();
                    }
                    // Ahora eliminamos los chats usando el método del repositorio
                    return chatRepository.deleteAllChatsByUserUID(userUID);
                })
                .onErrorMap(e -> new ChatsDeletionException(
                        String.format("Failed to delete chats and messages for user '%s'", userUID), e));
    }

    /**
     * Publica eventos de eliminación para cada chat eliminado.
     */
    private Flux<ChatModel> publishChatDeletionEvents(ChatModel deletedChat) {
        Mono<Void> publishToUser1 = publisher.publishEvent(PublishEventType.DELETE, deletedChat, "chat", deletedChat.getChatId(), deletedChat.getUser1Id());
        Mono<Void> publishToUser2 = publisher.publishEvent(PublishEventType.DELETE, deletedChat, "chat", deletedChat.getChatId(), deletedChat.getUser2Id());

        return Mono.when(publishToUser1, publishToUser2)
                   .thenReturn(deletedChat)
                   .flux()
                   .onErrorResume(e -> {
                       log.warn("Failed to publish chat deletion events for chat '{}': {}", 
                               deletedChat.getChatId(), e.getMessage());
                       // Continuamos con éxito ya que el chat fue eliminado correctamente
                       return Flux.just(deletedChat);
                   });
    }

    /**
     * Excepción personalizada para errores durante la eliminación de chats.
     */
    public static class ChatsDeletionException extends RuntimeException {
        public ChatsDeletionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
