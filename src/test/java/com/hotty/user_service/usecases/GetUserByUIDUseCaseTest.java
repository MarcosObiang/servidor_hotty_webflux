package com.hotty.user_service.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotty.user_service.DTOs.UserDTO;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.usecases.GetUserByUIDUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByUIDUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    @InjectMocks
    private GetUserByUIDUseCase getUserByUIDUseCase;

    private UserDataModel testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDataModel();
        testUser.setUserUID("test-uid");
        testUser.setName("Test User");
    }

    @Test
    void testExecute_Success() {
        when(userModelRepository.findByUserUID("test-uid"))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(getUserByUIDUseCase.executeWithDTO("test-uid"))
                .expectNextMatches(userDTO -> userDTO.getUserUID().equals("test-uid"))
                .verifyComplete();
    }

    @Test
    void testExecute_UserNotFound() {
        when(userModelRepository.findByUserUID("not-exist"))
                .thenReturn(Mono.error(new NoSuchElementException("User not found")));

        StepVerifier.create(getUserByUIDUseCase.executeWithDTO("not-exist"))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}