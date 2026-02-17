package com.hotty.chat_service.usecases;

import com.hotty.chat_service.interfaces.ChatRepository;
import com.hotty.common.services.EventPublishers.ChatEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case para actualizar la imagen de perfil de un usuario en todos los
 * chats.
 * 
 * Responsabilidades:
 * 1. Actualiza la imagen en todos los chats donde el usuario participa
 * 2. Publica evento de actualización para notificar cambios
 * 3. Mantiene la coherencia de datos entre usuario y chats
 */
@Service
public class UpdateUserPictureInChatsUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserPictureInChatsUseCase.class);

    private final ChatRepository chatRepository;
    private final ChatEventPublisher chatEventPublisher;

    public UpdateUserPictureInChatsUseCase(ChatRepository chatRepository, ChatEventPublisher chatEventPublisher) {
        this.chatRepository = chatRepository;
        this.chatEventPublisher = chatEventPublisher;
    }

    /**
     * Actualiza la imagen de un usuario en todos los chats donde participa.
     * 
     * @param userUID El UID del usuario que actualizó su imagen
     * @param newPictureUrl La nueva URL de la imagen de perfil
     * @return Mono<Long> número de chats actualizados
     */
    public Mono<Long> execute(String userUID, String newPictureUrl) {
        if (userUID == null || userUID.isBlank()) {
            return Mono.error(new IllegalArgumentException("UserUID no puede ser nulo o vacío"));
        }

        if (newPictureUrl == null) {
            return Mono.error(new IllegalArgumentException("Nueva URL de imagen no puede ser nula"));
        }

        log.debug("🔄 Updating user picture in chats for user: {} with new URL: {}", userUID, newPictureUrl);

        return chatRepository.updateUserPictureInAllChats(userUID, newPictureUrl)
                .flatMap(updatedChat -> {
                    // Publicar evento general de actualización
                    return Mono.zip(
                        chatEventPublisher.publishChatUpdated(updatedChat, updatedChat.getUser1Id()), 
                        chatEventPublisher.publishChatUpdated(updatedChat, updatedChat.getUser2Id())
                    ).then(Mono.just(updatedChat));
                })
                .doOnError(e -> 
                    log.warn("Failed to publish chat update event for user: {}", userUID, e))
                .onErrorResume(e -> Mono.empty()) // No fallar por errores de publicación
                .count() // Contar cuántos chats se actualizaron
                .doOnSuccess(count -> 
                    log.info("✅ Successfully updated {} chats for user: {}", count, userUID))
                .doOnError(error -> 
                    log.error("💥 Error updating chats for user: {} - {}", userUID, error.getMessage()));
    }
}
