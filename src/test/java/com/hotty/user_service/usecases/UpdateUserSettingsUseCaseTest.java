package com.hotty.user_service.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserSettingsModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.usecases.UpdateUserSettingsUseCase;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserSettingsUseCaseTest {

    @Mock
    private UserModelRepository userModelRepository;

    @InjectMocks
    private UpdateUserSettingsUseCase updateUserSettingsUseCase;

    private UserDataModel testUser;
    private UserSettingsModel testSettings;

    @BeforeEach
    void setUp() {
        testUser = new UserDataModel();
        testUser.setUserUID("test-uid");
        testUser.setName("Test User");

        testSettings = new UserSettingsModel();
        testSettings.setIsDarkModeEnabled(true);
        testSettings.setMinAge(25);
        testSettings.setMaxAge(35);
    }

    @Test
    void testExecute_Success() {
        testUser.setSettings(testSettings);
        
        when(userModelRepository.updateSettings("test-uid", testSettings))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(updateUserSettingsUseCase.execute("test-uid", testSettings))
                .expectNextMatches(user -> user.getSettings().getIsDarkModeEnabled())
                .verifyComplete();
    }

    @Test
    void testExecute_UserNotFound() {
        when(userModelRepository.updateSettings(anyString(), any(UserSettingsModel.class)))
                .thenReturn(Mono.error(new NoSuchElementException("User not found")));

        StepVerifier.create(updateUserSettingsUseCase.execute("not-exist", testSettings))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}