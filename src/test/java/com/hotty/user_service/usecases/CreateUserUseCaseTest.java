package com.hotty.user_service.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.usecases.CreateUserUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    private UserDataModel testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDataModel();
        testUser.setUserUID("test-uid");
        testUser.setName("Test User");
    }

    @Test
    void testExecute_Success() {
        when(userModelRepository.save(any(UserDataModel.class)))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(createUserUseCase.execute(testUser))
                .expectNextMatches(user -> user.getUserUID().equals("test-uid"))
                .verifyComplete();
    }

    @Test
    void testExecute_RepositoryError() {
        when(userModelRepository.save(any(UserDataModel.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(createUserUseCase.execute(testUser))
                .expectError(RuntimeException.class)
                .verify();
    }
}