package com.hotty.chat_service.usecases.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hotty.chat_service.interfaces.ChatRepository;
import com.hotty.chat_service.model.ChatModel;
import com.hotty.common.enums.PublishEventType;
import com.hotty.common.services.ChatEventPublisher;
import com.hotty.chat_service.usecases.messages.DeleteMessagesUseCase;

import reactor.core.publisher.Mono;

/**
 * Caso de uso para eliminar un chat específico por su UID.
 * Incluye eliminación en cascada de todos los mensajes asociados al chat.
 * Utiliza el repositorio ChatRepository para acceder a la base de datos.
 */

 @Service
public class DeleteChatUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteChatUseCase.class);
    
    private final ChatRepository chatRepository;
    private final ChatEventPublisher publisher;
    private final DeleteMessagesUseCase deleteMessagesUseCase;

    public DeleteChatUseCase(
            ChatRepository chatRepository, 
            ChatEventPublisher publisher,
            DeleteMessagesUseCase deleteMessagesUseCase) {
        this.chatRepository = chatRepository;
        this.publisher = publisher;
        this.deleteMessagesUseCase = deleteMessagesUseCase;
    }

    /**
     * Elimina un chat específico y todos sus mensajes asociados.
     * 
     * @param chatUID El UID del chat a eliminar
     * @return Un Mono que emite el ChatModel eliminado
     */
    public Mono<ChatModel> execute(String chatUID) {
        if (chatUID == null || chatUID.trim().isEmpty()) {
            log.warn("Attempted to delete chat with null or blank chatUID");
            return Mono.error(new IllegalArgumentException("ChatUID cannot be null or blank"));
        }

        return deleteMessagesAndChat(chatUID)
                .flatMap(this::publishChatDeletionEvents)
                .doOnSuccess(deletedChat -> 
                        log.info("Successfully deleted chat '{}' and all its messages", chatUID))
                .doOnError(error -> 
                        log.error("Failed to delete chat '{}': {}", chatUID, error.getMessage(), error));
    }

    /**
     * Elimina primero todos los mensajes del chat y luego el chat mismo.
     * Si falla la eliminación de mensajes, no se procede a eliminar el chat.
     */
    private Mono<ChatModel> deleteMessagesAndChat(String chatUID) {
        return deleteMessagesUseCase.execute(chatUID)
                .doOnSuccess(ignored -> log.debug("Messages deleted successfully for chat '{}'", chatUID))
                .then(Mono.defer(() -> chatRepository.deleteById(chatUID)))
                .onErrorMap(e -> {
                    if (e instanceof ChatDeletionException) {
                        return e; // Ya está mapeado, no lo re-mapees
                    }
                    return new ChatDeletionException(
                            String.format("Failed to delete chat '%s' and/or its messages", chatUID), e);
                });
    }

    /**
     * Publica eventos de eliminación del chat a ambos participantes.
     */
    private Mono<ChatModel> publishChatDeletionEvents(ChatModel deletedChat) {
        Mono<Void> publishToUser1 = publisher.publishChatDeleted(deletedChat, deletedChat.getUser1Id());
        Mono<Void> publishToUser2 = publisher.publishChatDeleted(deletedChat, deletedChat.getUser2Id());

        return Mono.zip(publishToUser1, publishToUser2)
                   .thenReturn(deletedChat)
                   .onErrorResume(e -> {
                       log.warn("Failed to publish chat deletion events for chat '{}': {}", 
                               deletedChat.getChatId(), e.getMessage());
                       // Continuamos con éxito ya que el chat fue eliminado correctamente
                       return Mono.just(deletedChat);
                   });
    }

    /**
     * Excepción personalizada para errores durante la eliminación del chat.
     */
    public static class ChatDeletionException extends RuntimeException {
        public ChatDeletionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
