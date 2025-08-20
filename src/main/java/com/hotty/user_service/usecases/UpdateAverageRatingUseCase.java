package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.services.UserEventPublisherService;

import reactor.core.publisher.Mono;

@Component
public class UpdateAverageRatingUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public UpdateAverageRatingUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Actualiza la calificación promedio de un usuario.
     *
     * @param userUID El UID del usuario cuya calificación promedio se actualizará.
     * @param averageRating La nueva calificación promedio a establecer.
     * @return Un Mono que emite el UserDataModel actualizado.
     */
    public Mono<UserDataModel> execute(String userUID, Integer averageRating) {
        return userModelRepository.updateProfileAverageRating(userUID, averageRating)
                .flatMap(updatedUser ->
                    eventPublisher.publishUserUpdated(updatedUser)
                                  .thenReturn(updatedUser)
                );
    }
    
}
