package com.hotty.likes_service.usecases;

import org.springframework.stereotype.Service;
import com.hotty.likes_service.exceptions.AccessDeniedException;
import com.hotty.common.enums.PublishEventType;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.likes_service.repository.LikesRepo;
import com.hotty.common.services.LikeEventPublisher;

import reactor.core.publisher.Mono;

@Service
public class DeleteLikeUseCase {

    private final LikesRepo likesRepo;
    private final LikeEventPublisher publisher;

    public DeleteLikeUseCase(LikesRepo likesRepo, LikeEventPublisher publisher) {
        this.likesRepo = likesRepo;
        this.publisher = publisher;
    }

    public Mono<Void> execute(String userUID, String likeUID) {
        return likesRepo.findByLikeUID(likeUID)
                .flatMap(likeToDelete -> {
                    // 1. Comprobar permisos: solo el receptor puede borrar el like.
                    // if (!likeToDelete.getReceiverUID().equals(userUID)) {
                    //     return Mono.error(new AccessDeniedException("El usuario no tiene permiso para borrar este like."));
                    // }

                    // 2. Si tiene permiso, proceder a borrar y LUEGO publicar el evento.
                    // El operador 'then' espera a que 'delete' termine y luego ejecuta 'publishUserDeleted'.
                    // Toda la cadena devuelve un Mono<Void> que se completa cuando ambas operaciones terminan.
                    return likesRepo.delete(likeUID)
                            .then(publisher.publishUserDeleted(likeToDelete).then());
                });
    }
}
