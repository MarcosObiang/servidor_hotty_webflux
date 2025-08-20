package com.hotty.user_service.usecases;

import com.hotty.user_service.model.UserCharacteristicsModel;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.services.UserEventPublisherService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class UpdateFilterCharacteristicsUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;
    private static final List<String> VALID_SEX_PREFERENCES = Arrays.asList("Male", "Female", "Both");

    public UpdateFilterCharacteristicsUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<UserDataModel> execute(String userUID, UserCharacteristicsModel characteristics, Integer maxAge, Integer minAge, String preferredSex, Integer searchRadiusInKm) {
        if (minAge == null || maxAge == null || minAge < 18 || maxAge > 100 || minAge > maxAge) {
            return Mono.error(new IllegalArgumentException("Rango de edad inválido. minAge debe ser >= 18, maxAge <= 100 y minAge <= maxAge."));
        }
        if (preferredSex == null || !VALID_SEX_PREFERENCES.contains(preferredSex)) {
            return Mono.error(new IllegalArgumentException("Preferencia de sexo inválida. Valores permitidos: " + VALID_SEX_PREFERENCES));
        }

        return userModelRepository.updateFilterCharacteristics(userUID, characteristics, maxAge, minAge, preferredSex, searchRadiusInKm)
                .flatMap(updatedUser ->
                        eventPublisher.publishUserUpdated(updatedUser)
                                .thenReturn(updatedUser)
                );
    }
}