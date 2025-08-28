package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

@Component
public class UpdateDeviceNotificationToken {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService userEventPublisherService;

    public UpdateDeviceNotificationToken(UserModelRepository userModelRepository,
            UserEventPublisherService userEventPublisherService) {
        this.userModelRepository = userModelRepository;
        this.userEventPublisherService = userEventPublisherService;
    }

    public Mono<UserDataModel> execute(String userUID, String deviceNotificationToken) {
        return userModelRepository.updateDeviceNotificationToken(userUID, deviceNotificationToken)
                .flatMap(updatedUser -> userEventPublisherService.publishUserUpdated(updatedUser)
                        .thenReturn(updatedUser));
    }
}
