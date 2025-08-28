package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.model.UserCharacteristicsModel;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

@Component
public class UpdateUserCharacteristicsUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public UpdateUserCharacteristicsUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Ejecuta la lógica de negocio para actualizar las características de un usuario.
     * Delega la operación de persistencia al repositorio.
     *
     * @param userUID El UID del usuario a actualizar.
     * @param characteristics El nuevo objeto UserCharacteristicsModel.
     * @return Un Mono que emite la entidad UserDataModel completa y actualizada.
     */
    public Mono<UserDataModel> execute(String userUID, UserCharacteristicsModel characteristics) {
        return userModelRepository.updateCharacteristics(userUID, characteristics)
                .flatMap(updatedUser ->
                    eventPublisher.publishUserUpdated(updatedUser)
                                  .thenReturn(updatedUser)
                );
    }
}