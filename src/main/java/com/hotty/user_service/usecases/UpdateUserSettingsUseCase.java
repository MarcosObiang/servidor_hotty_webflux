package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserSettingsModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

@Component
public class UpdateUserSettingsUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    // Inyectamos la interfaz, no la implementación concreta.
    // Spring buscará un bean que implemente esta interfaz (UserSettingsMongoRepository) y lo inyectará.
    public UpdateUserSettingsUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Ejecuta la lógica de negocio para actualizar las configuraciones de un usuario.
     * Este caso de uso no tiene conocimiento de la base de datos subyacente.
     * Simplemente delega la operación de persistencia al repositorio.
     *
     * @param userUID El UID del usuario a actualizar.
     * @param settings El nuevo objeto UserSettingsModel.
     * @return Un Mono que emite la entidad UserDataModel completa y actualizada.
     */
    public Mono<UserDataModel> execute(String userUID, UserSettingsModel settings) {
        // El caso de uso ahora solo llama al método definido en la interfaz.
        return userModelRepository.updateSettings(userUID, settings)
                .flatMap(updatedUser ->
                    eventPublisher.publishUserUpdated(updatedUser)
                                  .thenReturn(updatedUser)
                );
    }
}