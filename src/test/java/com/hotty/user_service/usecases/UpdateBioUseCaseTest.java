package com.hotty.user_service.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.usecases.UpdateBioUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBioUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    @InjectMocks
    private UpdateBioUseCase updateBioUseCase;

    private UserDataModel testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDataModel();
        testUser.setUserUID("test-uid");
        testUser.setUserBio("Nueva bio");
    }

    @Test
    void testExecute_Success() {
        when(userModelRepository.updateBio("test-uid", "Nueva bio"))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(updateBioUseCase.execute("test-uid", "Nueva bio"))
                .expectNextMatches(user -> "Nueva bio".equals(user.getUserBio()))
                .verifyComplete();
    }

    @Test
    void testExecute_UserNotFound() {
        when(userModelRepository.updateBio(anyString(), anyString()))
                .thenReturn(Mono.error(new NoSuchElementException("User not found")));

        StepVerifier.create(updateBioUseCase.execute("not-exist", "bio"))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}