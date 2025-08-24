package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.common.services.UserEventPublisherService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

@Component
public class UpdateUserImagesUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public UpdateUserImagesUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<UserDataModel> execute(String userUID, String userImage1, String userImage2, String userImage3, String userImage4, String userImage5, String userImage6) { // Changed return type to UserDataModel
        return userModelRepository.updateImages(userUID, userImage1, userImage2, userImage3, userImage4, userImage5, userImage6)
                .switchIfEmpty(Mono.error(new RuntimeException("No se encontró un usuario con el UID: " + userUID)))
                .flatMap(updatedUser ->
                    eventPublisher.publishUserUpdated(updatedUser)
                                  .thenReturn(updatedUser)
                );
    }

}
