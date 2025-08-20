package com.hotty.user_service.usecases;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.services.UserEventPublisherService;

import reactor.core.publisher.Mono;

@Service
public class UpdateProfileDiscoverySettingsUseCase {
    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public UpdateProfileDiscoverySettingsUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    // CORRECCIÓN: La firma del método ahora devuelve el modelo actualizado.
    public Mono<UserDataModel> execute(String userUID, Map<String, Object> settings) {

        if (settings == null || settings.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Settings cannot be null or empty"));
        }

        if( !settings.containsKey("isVisibleToOtherUsers") ||
            !settings.containsKey("searchRadiusInKm") ||
            !settings.containsKey("minAge") ||
            !settings.containsKey("maxAge") ||
            !settings.containsKey("sexPreference")) {
            return Mono.error(new IllegalArgumentException("Missing required settings keys"));
        }
        

        return userModelRepository.updateProfileDiscoverySettings(userUID, settings)
                .flatMap(updatedUser ->
                    eventPublisher.publishUserUpdated(updatedUser)
                                  .thenReturn(updatedUser)
                );
    }

}
