package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

@Component
public class UpdateBioUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public UpdateBioUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Actualiza la biografía de un usuario.
     *
     * @param userUID El UID del usuario a actualizar.
     * @param bio El nuevo texto de la biografía.
     * @return Un Mono<UserDataModel> que contiene el usuario actualizado.
     */
    public Mono<UserDataModel> execute(String userUID, String bio) {
        return userModelRepository.updateBio(userUID, bio)
                .switchIfEmpty(Mono.error(new RuntimeException("No se encontró un usuario con el UID: " + userUID)))
                .flatMap(updatedUser ->
                    eventPublisher.publishUserUpdated(updatedUser)
                                  .thenReturn(updatedUser)
                );
    }
    
}
