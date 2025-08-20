package com.hotty.user_service.usecases;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.services.UserEventPublisherService;

import reactor.core.publisher.Mono;

@Component
public class UpdateUserLocationUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public UpdateUserLocationUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Actualiza la localización de un usuario.
     *
     * @param userUID El UID del usuario a actualizar.
     * @param location El nuevo punto GeoJsonPoint.
     * @return Un Mono<UserDataModel> que emite el usuario actualizado.
     */
    public Mono<UserDataModel> execute(String userUID, GeoJsonPoint location) {
        // Nota: Se asume que userModelRepository.updateLocationData ha sido modificado
        // para devolver Mono<UserDataModel> en lugar de Mono<Void>.
        return userModelRepository.updateLocationData(userUID, location)
                .flatMap(updatedUser ->
                    eventPublisher.publishUserUpdated(updatedUser)
                                  .thenReturn(updatedUser)
                );
    }
}