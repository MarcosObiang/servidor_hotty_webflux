package com.hotty.likes_service.usecases;

import java.time.LocalDate;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import com.hotty.likes_service.exceptions.AccessDeniedException;
import com.hotty.common.enums.PublishEventType;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.likes_service.repository.LikesRepo;
import com.hotty.common.services.LikeEventPublisher;

import reactor.core.publisher.Mono;

@Service
public class UpdateLikeUseCase {
    private final LikesRepo likesRepo;
    private final LikeEventPublisher publisher;

    public UpdateLikeUseCase(LikesRepo likesRepo, LikeEventPublisher publisher) {
        this.likesRepo = likesRepo;
        this.publisher = publisher;
    }

    /**
     * Actualiza un like. Actualmente, solo permite actualizar el estado
     * 'isRevealed'.
     * Solo el receptor del like tiene permiso para actualizarlo.
     *
     * @param likeUID    El UID del like a actualizar.
     * @param userUID    El UID del usuario que realiza la acción (debe ser el
     *                   receptor).
     * @param isRevealed El nuevo estado para 'isRevealed'.
     * @return Un Mono que emite el LikeModel actualizado.
     */
    public Mono<LikeModel> execute(String likeUID, String userUID, boolean isRevealed,String senderPictureUrl,LocalDate senderBirthDate,String senderName) {
        return likesRepo.findByLikeUID(likeUID)
                .flatMap(likeToUpdate -> {
                    // 1. Comprobar permisos: solo el receptor puede actualizar.
                    if (!likeToUpdate.getReceiverUID().equals(userUID)) {
                        return Mono.error(
                                new AccessDeniedException("El usuario no tiene permiso para actualizar este like."));
                    }

                    // 2. Actualizar el campo y guardar.
                    likeToUpdate.setIsRevealed(true);
                    likeToUpdate.setSenderPictureURL(senderPictureUrl);
                    likeToUpdate.setSenderBirthDate(senderBirthDate);
                    likeToUpdate.setSenderName(senderName);

                    return likesRepo.add(likeToUpdate) // 'add' funciona como save/update
                            .flatMap(updatedLike ->
                                    // Encadenamos la publicación y devolvemos el like actualizado.
                                    publisher.publishUserUpdated(updatedLike).thenReturn(updatedLike));
                });
    }

}
