package com.hotty.user_service.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.usecases.UpdateUserImagesUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserImagesUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    @InjectMocks
    private UpdateUserImagesUseCase updateUserImagesUseCase;

    private UserDataModel testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDataModel();
        testUser.setUserUID("test-uid");
        testUser.setUserImage1("img1");
        testUser.setUserImage6("img6");
    }

    @Test
    void testExecute_Success() {
        when(userModelRepository.updateImages("test-uid", "img1", "img2", "img3", "img4", "img5", "img6"))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(updateUserImagesUseCase.execute("test-uid", "img1", "img2", "img3", "img4", "img5", "img6"))
                .expectNextMatches(user -> "img6".equals(user.getUserImage6()))
                .verifyComplete();
    }

    @Test
    void testExecute_UserNotFound() {
        when(userModelRepository.updateImages(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new NoSuchElementException("User not found")));

        StepVerifier.create(updateUserImagesUseCase.execute("not-exist", "i1", "i2", "i3", "i4", "i5", "i6"))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}