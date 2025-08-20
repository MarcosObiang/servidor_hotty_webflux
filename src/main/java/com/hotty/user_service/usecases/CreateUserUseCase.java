package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.services.UserEventPublisherService;

import reactor.core.publisher.Mono;

@Component
public class CreateUserUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public CreateUserUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<UserDataModel> execute(UserDataModel user) {
        return userModelRepository.save(user)
                .flatMap(savedUser ->
                    eventPublisher.publishUserCreated(savedUser)
                                  .thenReturn(savedUser) // Devuelve el usuario después de publicar
                );
    }

}
