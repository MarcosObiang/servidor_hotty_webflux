package com.hotty.user_service.usecases;

import org.springframework.stereotype.Service;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

/**
 * Use case for renewing user credits.
 * This use case checks if the user can claim their daily reward and updates
 * their profile accordingly.
 */

@Service
public class RenewUserCreditsUseCase {
    private static final int CREDITS_INCREMENT = 600; // Define how many credits to add

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService userEventPublisherService;

    public RenewUserCreditsUseCase(UserModelRepository userModelRepository,
            UserEventPublisherService userEventPublisherService) {
        this.userModelRepository = userModelRepository;
        this.userEventPublisherService = userEventPublisherService;
    }

    public Mono<Object> execute(String userUID) {
        return userModelRepository.findByUserUID(userUID)
                .flatMap(userData -> {
                    Long nextDailyRewardTimestamp = userData.getRewards().getNextDailyRewardTimestamp();
                    Long currentTimestamp = System.currentTimeMillis();

                    if (nextDailyRewardTimestamp == null || currentTimestamp >= nextDailyRewardTimestamp) {
                        // El usuario puede reclamar la recompensa
                        return userModelRepository.updateProfileCredits(userUID, CREDITS_INCREMENT,
                                0L, false).flatMap(newUserData -> {
                                 return   userEventPublisherService.publishUserUpdated(newUserData).thenReturn(userData);
                                });
                    } else {
                        // El usuario ya reclamó su recompensa diaria
                        long timeRemainingMs = nextDailyRewardTimestamp - currentTimestamp;
                        long hoursRemaining = timeRemainingMs / (60 * 60 * 1000);
                        long minutesRemaining = (timeRemainingMs % (60 * 60 * 1000)) / (60 * 1000);

                        String errorMessage = String.format(
                                "Daily reward already claimed. Next reward available in %d hours and %d minutes.",
                                hoursRemaining, minutesRemaining);

                        return Mono.error(new IllegalArgumentException(errorMessage));
                    }
                });
    }

}
