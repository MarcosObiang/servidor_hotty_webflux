package com.hotty.user_service.usecases;

import com.hotty.user_service.DTOs.UserDTOwithDistance;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByPositionUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    private GetUserByPositionUseCase getUserByPositionUseCase;

    @BeforeEach
    void setUp() {
        getUserByPositionUseCase = new GetUserByPositionUseCase(userModelRepository);
    }

    @Test
    @DisplayName("Execute should return users when valid characteristics are provided")
    void execute_shouldReturnUsers_whenValidCharacteristics() {
        // Given
        double latitude = 40.7128;
        double longitude = -74.0060;
        double radiusInKm = 10.0;
        HashMap<String, Object> characteristics = new HashMap<>();
        characteristics.put("smoke", "NON_SMOKER");
        Integer minAge = 18;
        Integer maxAge = 99;
        String preferredSex = "Female";

        GeoJsonPoint expectedPoint = new GeoJsonPoint(longitude, latitude);
        Distance expectedDistance = new Distance(radiusInKm, Metrics.KILOMETERS);
        UserDTOwithDistance user1 = new UserDTOwithDistance();

        // CORRECCIÓN: El mock debe esperar los parámetros correctos.
        when(userModelRepository.findByLocationNear(eq(expectedPoint), eq(expectedDistance), eq(characteristics), eq(maxAge), eq(minAge), eq(preferredSex)))
                .thenReturn(Flux.just(user1));

        // When
        // CORRECCIÓN: La llamada al método debe incluir todos los parámetros.
        Flux<UserDTOwithDistance> result = getUserByPositionUseCase.execute(latitude, longitude, radiusInKm, characteristics, maxAge, minAge, preferredSex);

        // Then
        StepVerifier.create(result)
                .expectNext(user1)
                .verifyComplete();

        // CORRECCIÓN: La verificación debe usar los parámetros de la prueba, no valores hardcodeados.
        verify(userModelRepository).findByLocationNear(expectedPoint, expectedDistance, characteristics, maxAge, minAge, preferredSex);
    }

    @Test
    @DisplayName("Execute should return users when characteristics map is null")
    void execute_shouldReturnUsers_whenCharacteristicsAreNull() {
        // Given
        double latitude = 40.7128;
        double longitude = -74.0060;
        double radiusInKm = 10.0;
        Integer minAge = 18;
        Integer maxAge = 99;
        String preferredSex = "Both";

        GeoJsonPoint expectedPoint = new GeoJsonPoint(longitude, latitude);
        Distance expectedDistance = new Distance(radiusInKm, Metrics.KILOMETERS);
        UserDTOwithDistance user1 = new UserDTOwithDistance();

        when(userModelRepository.findByLocationNear(eq(expectedPoint), eq(expectedDistance), eq(null), eq(maxAge), eq(minAge), eq(preferredSex)))
                .thenReturn(Flux.just(user1));

        // When
        Flux<UserDTOwithDistance> result = getUserByPositionUseCase.execute(latitude, longitude, radiusInKm, null, maxAge, minAge, preferredSex);

        // Then
        StepVerifier.create(result)
                .expectNext(user1)
                .verifyComplete();
    }

    @Test
    @DisplayName("Execute should return an error when an invalid characteristic value is provided")
    void execute_shouldReturnError_whenInvalidCharacteristicValue() {
        // Given
        double latitude = 40.7128;
        double longitude = -74.0060;
        double radiusInKm = 10.0;
        HashMap<String, Object> characteristics = new HashMap<>();
        characteristics.put("smoke", "INVALID_VALUE"); // Invalid value

        // When
        Flux<UserDTOwithDistance> result = getUserByPositionUseCase.execute(latitude, longitude, radiusInKm, characteristics, 18, 99, "Both");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        "Invalid characteristics provided".equals(throwable.getMessage()))
                .verify();

        verify(userModelRepository, never()).findByLocationNear(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Execute should return an error when an invalid characteristic key is provided")
    void execute_shouldReturnError_whenInvalidCharacteristicKey() {
        // Given
        double latitude = 40.7128;
        double longitude = -74.0060;
        double radiusInKm = 10.0;
        HashMap<String, Object> characteristics = new HashMap<>();
        characteristics.put("invalidKey", "someValue"); // Invalid key

        // When
        Flux<UserDTOwithDistance> result = getUserByPositionUseCase.execute(latitude, longitude, radiusInKm, characteristics, 18, 99, "Both");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        "Invalid characteristics provided".equals(throwable.getMessage()))
                .verify();

        verify(userModelRepository, never()).findByLocationNear(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Execute should proceed when characteristics map is empty")
    void execute_shouldProceed_whenCharacteristicsAreEmpty() {
        // Given
        double latitude = 40.7128;
        double longitude = -74.0060;
        double radiusInKm = 10.0;
        Integer minAge = 18;
        Integer maxAge = 99;
        String preferredSex = "Both";
        HashMap<String, Object> emptyCharacteristics = new HashMap<>();

        when(userModelRepository.findByLocationNear(any(), any(), eq(emptyCharacteristics), eq(maxAge), eq(minAge), eq(preferredSex)))
                .thenReturn(Flux.empty());

        // When
        Flux<UserDTOwithDistance> result = getUserByPositionUseCase.execute(latitude, longitude, radiusInKm, emptyCharacteristics, maxAge, minAge, preferredSex);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userModelRepository).findByLocationNear(any(), any(), eq(emptyCharacteristics), eq(maxAge), eq(minAge), eq(preferredSex));
    }
}