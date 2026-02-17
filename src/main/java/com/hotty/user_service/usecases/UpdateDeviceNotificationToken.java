package com.hotty.user_service.usecases;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Component;

import com.hotty.common.enums.NotificationProvider;
import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.enums.LocalizationCodes;
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

    public Mono<UserDataModel> execute(String userUID, String deviceNotificationToken, NotificationProvider provider, LocalizationCodes locale) {

        return userModelRepository.findUserByNotificationToken(deviceNotificationToken)
                .flatMap(existingUser -> {
                    if (userUID.equals(existingUser.getUserUID())) {
                        // El token ya está asociado al mismo usuario
                        // Verificar si necesita actualizar el provider
                        // Verificar si debe actualizar locale
                        if (provider != existingUser.getNotificationData().getProvider()|| locale != existingUser.getNotificationData().getLocale()) {
                            return userModelRepository.updateDeviceNotificationToken(userUID, deviceNotificationToken, provider, locale)
                                    .flatMap(updatedUser -> userEventPublisherService.publishUserUpdated(updatedUser)
                                            .thenReturn(updatedUser));
                        } else {
                            // Token y provider son iguales, no hacer nada
                            return Mono.just(existingUser);
                        }
                    } else {
                        // El token está asociado a otro usuario, limpiarlo y asignar al usuario objetivo
                        return userModelRepository.updateDeviceNotificationToken(existingUser.getUserUID(), "", null, locale)
                                .then(userModelRepository.updateDeviceNotificationToken(userUID, deviceNotificationToken, provider, locale))
                                .flatMap(updatedUser -> userEventPublisherService.publishUserUpdated(updatedUser)
                                        .thenReturn(updatedUser));
                    }
                })
                .onErrorResume(error -> {
                    if (error instanceof NoSuchElementException) {
                        // Token no existe, asignarlo directamente al usuario
                        return userModelRepository.updateDeviceNotificationToken(userUID, deviceNotificationToken, provider, locale)
                                .flatMap(updatedUser -> userEventPublisherService.publishUserUpdated(updatedUser)
                                        .thenReturn(updatedUser));
                    }
                    return Mono.error(error);
                });

    }
}
