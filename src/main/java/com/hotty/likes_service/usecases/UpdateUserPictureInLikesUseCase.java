package com.hotty.likes_service.usecases;

import com.hotty.common.services.EventPublishers.LikeEventPublisher;
import com.hotty.likes_service.repository.LikesRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case para actualizar la imagen de perfil de un usuario en todos los likes.
 * 
 * Responsabilidades:
 * 1. Actualiza la imagen en todos los likes donde el usuario aparece como sender
 * 2. Publica evento de actualización para notificar cambios
 * 3. Mantiene la coherencia de datos entre usuario y likes
 */
@Service
public class UpdateUserPictureInLikesUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserPictureInLikesUseCase.class);

    private final LikesRepo likesRepo;
    private final LikeEventPublisher likeEventPublisher;

    public UpdateUserPictureInLikesUseCase(LikesRepo likesRepo, LikeEventPublisher likeEventPublisher) {
        this.likesRepo = likesRepo;
        this.likeEventPublisher = likeEventPublisher;
    }

    /**
     * Actualiza la imagen de un usuario en todos los likes donde aparece como sender.
     * 
     * @param senderUID El UID del usuario que actualizó su imagen
     * @param newPictureUrl La nueva URL de la imagen de perfil
     * @return Mono<Long> número de likes actualizados
     */
    public Mono<Long> execute(String senderUID, String newPictureUrl) {
        if (senderUID == null || senderUID.isBlank()) {
            return Mono.error(new IllegalArgumentException("SenderUID no puede ser nulo o vacío"));
        }

        if (newPictureUrl == null) {
            return Mono.error(new IllegalArgumentException("Nueva URL de imagen no puede ser nula"));
        }

        log.debug("🔄 Updating sender picture in likes for user: {} with new URL: {}", senderUID, newPictureUrl);

        return likesRepo.updateSenderPictureInAllLikes(senderUID, newPictureUrl)
                .flatMap(updatedLike -> {
                    // Publicar evento de actualización para cada like modificado
                    return likeEventPublisher.publishLikeUpdated(updatedLike)
                            .doOnError(e -> 
                                log.warn("Failed to publish like update event for likeUID: {}", 
                                        updatedLike.getLikeUID(), e))
                            .onErrorResume(e -> Mono.empty()) // No fallar por errores de publicación
                            .thenReturn(updatedLike);
                })
                .count() // Contar cuántos likes se actualizaron
                .doOnSuccess(count -> 
                    log.info("✅ Successfully updated {} likes for sender: {}", count, senderUID))
                .doOnError(error -> 
                    log.error("💥 Error updating likes for sender: {} - {}", senderUID, error.getMessage()));
    }
}
