package com.hotty.user_service.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.usecases.UpdateUserLocationUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserLocationUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    @InjectMocks
    private UpdateUserLocationUseCase updateUserLocationUseCase;

    private GeoJsonPoint testLocation;

    @BeforeEach
    void setUp() {
        testLocation = new GeoJsonPoint(-3.7038, 40.4168);
    }

    @Test
    void testExecute_Success() {
        when(userModelRepository.updateLocationData("test-uid", testLocation))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateUserLocationUseCase.execute("test-uid", testLocation))
                .verifyComplete();
    }

    @Test
    void testExecute_UserNotFound() {
        when(userModelRepository.updateLocationData(anyString(), any(GeoJsonPoint.class)))
                .thenReturn(Mono.error(new NoSuchElementException("User not found")));

        StepVerifier.create(updateUserLocationUseCase.execute("not-exist", testLocation))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}