package com.hotty.user_service.usecases;

import org.springframework.stereotype.Service;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

/**
 * Use case for claiming the first reward for a user.
 * This use case checks if the user is eligible to claim their first reward
 * and updates their profile accordingly.
 */

 @Service
public class ClaimFirstRewardUseCase {

    public static final int FIRST_REWARD_CREDITS = 6000; // Define how many credits to add for the first reward

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService userEventPublisherService;

    public ClaimFirstRewardUseCase(UserModelRepository userModelRepository,
            UserEventPublisherService userEventPublisherService) {
        this.userModelRepository = userModelRepository;
        this.userEventPublisherService = userEventPublisherService;
    }

    public Mono<Object> execute(String userUID) {
        return userModelRepository.findByUserUID(userUID)
                .flatMap(userData -> {
                    if (userData.getRewards().getWaitingFirstReward()) {
                        // User can claim the first reward
                        return userModelRepository.updateFirstRewardCredits(userUID, FIRST_REWARD_CREDITS, false)
                                .flatMap(newUserData -> {
                                   return userEventPublisherService.publishUserUpdated(newUserData).thenReturn(newUserData);
                                });
                    } else {
                        // User has already claimed the first reward
                        return Mono.error(new IllegalArgumentException("First reward already claimed."));
                    }
                });
    }
    
}
