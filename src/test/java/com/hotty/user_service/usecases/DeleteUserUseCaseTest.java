package com.hotty.user_service.usecases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.usecases.DeleteUserUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteUserUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    @InjectMocks
    private DeleteUserUseCase deleteUserUseCase;

    @Test
    void testExecute_Success() {
        when(userModelRepository.deleteByUserUID("test-uid"))
                .thenReturn(Mono.empty());

        StepVerifier.create(deleteUserUseCase.execute("test-uid"))
                .verifyComplete();
    }

    @Test
    void testExecute_UserNotFound() {
        when(userModelRepository.deleteByUserUID("not-exist"))
                .thenReturn(Mono.error(new NoSuchElementException("User not found")));

        StepVerifier.create(deleteUserUseCase.execute("not-exist"))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void testExecute_DatabaseError() {
        when(userModelRepository.deleteByUserUID(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(deleteUserUseCase.execute("test-uid"))
                .expectError(RuntimeException.class)
                .verify();
    }
}