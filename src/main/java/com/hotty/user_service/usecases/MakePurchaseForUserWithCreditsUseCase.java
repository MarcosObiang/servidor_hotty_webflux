package com.hotty.user_service.usecases;

import java.sql.Date;

import org.springframework.stereotype.Service;

import com.hotty.common.services.UserEventPublisherService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

enum PurchaseType {
    REACTION_REVELATION,
    ANONYMOUS_CHAT
}

/*
 * Use case for making purchases with user credits.
 * This use case checks if the user has enough credits for the purchase,
 * and deducts the appropriate amount if they do.
 * It also handles the case where the user is a premium user,
 * in which case they can make purchases without checking credits.
 * It returns the updated UserDataModel after the purchase.
 * If the user has 200 or fewer credits after the purchase,
 * it updates the profile credits and returns the updated UserDataModel.
 */

@Service
public class MakePurchaseForUserWithCreditsUseCase {

    final UserModelRepository userModelRepository;
    final UserEventPublisherService userEventPublisherService;
    final static int REACTION_REVELATION_COST = 200; // Cost in credits for revealing a reaction
    final static int EXPIRED_REACTION_REVELATION_COST = 400; // Cost in credits for revealing an expired reaction
    final static int ANNONYMOUS_CHAT_COST = 150; // Cost in credits for starting an anonymous chat

    public MakePurchaseForUserWithCreditsUseCase(UserModelRepository userModelRepository,
            UserEventPublisherService userEventPublisherService) {
        this.userModelRepository = userModelRepository;
        this.userEventPublisherService = userEventPublisherService;
    }

    /*
     * Executes the purchase for a user with the specified UID and purchase type.
     * It checks if the user has enough credits and deducts the appropriate amount
     * if they do.
     * If the user is a premium user, it allows the purchase without checking
     * credits.
     * If the user has 200 or fewer credits after the purchase, it updates the
     * profile
     * credits and returns the updated UserDataModel.
     * 
     * @param userUID The UID of the user making the purchase.
     * 
     * @param purchaseType The type of purchase being made (e.g.,
     * REACTION_REVELATION, ANONYMOUS_CHAT).
     * 
     * @return A Mono that emits the updated UserDataModel after the purchase,
     * or an error if the user does not have enough credits or if the user is not
     * found.
     * 
     * @throws IllegalArgumentException if the user does not exist or does not have
     * enough credits for the purchase.
     */

    public Mono<UserDataModel> execute(String userUID, String purchaseType) {
        return userModelRepository.findByUserUID(userUID).flatMap(step1 -> {

            if (step1 == null) {
                return Mono.error(new IllegalArgumentException("User not found with UID: " + userUID));
            }

            if (step1.getRewards().getIsPremium()) {
                // If the user is premium, they can make purchases without checking credits
                return Mono.just(step1);
            }

            if (purchaseType.equals("REACTION_REVELATION")) {
                if (step1.getRewards().getCoins() < REACTION_REVELATION_COST) {
                    return Mono.error(new IllegalArgumentException("Not enough credits for reaction revelation."));
                }
            } else if (purchaseType.equals("EXPIRED_REACTION_REVELATION")) {
                if (step1.getRewards().getCoins() < EXPIRED_REACTION_REVELATION_COST) {
                    return Mono
                            .error(new IllegalArgumentException("Not enough credits for expired reaction revelation."));
                }
            } else if (purchaseType.equals("ANONYMOUS_CHAT")) {
                if (step1.getRewards().getCoins() < ANNONYMOUS_CHAT_COST) {
                    return Mono.error(new IllegalArgumentException("Not enough credits for anonymous chat."));
                }
            }

            int cost = purchaseType.equals("REACTION_REVELATION") ? REACTION_REVELATION_COST
                    : (purchaseType.equals("EXPIRED_REACTION_REVELATION") ? EXPIRED_REACTION_REVELATION_COST
                            : ANNONYMOUS_CHAT_COST);

            return userModelRepository
                    .substractCreditsFromUser(userUID, cost)
                    .flatMap(updatedUser -> {
                        // Check if the user has 200 or fewer credits after the purchase
                        if (updatedUser.getRewards().getCoins() <= 200
                                && !updatedUser.getRewards().getWaitingReward()) {
                            return userModelRepository
                                    .updateProfileCredits(userUID, updatedUser.getRewards().getCoins(),
                                            (System.currentTimeMillis() + 2 * 60 * 1000), true) // Set next daily reward
                                    // timestamp to 2 minutes
                                    // later in milliseconds (THIS
                                    // iS JUST A TEST, IT SHOULD
                                    // BE THE REAL TIMESTAMP WITH
                                    // IS // THE CURRENT TIME + 24
                                    // HOURS)
                                    .flatMap(finalUser -> {

                                        // Return the final updated user
                                        System.out.println(
                                                "Purchase successful. User updated: " + finalUser.getUserUID());

                                        return userEventPublisherService.publishUserUpdated(finalUser)
                                                .thenReturn(finalUser);
                                    });

                        } else {

                            System.out.println("Purchase successful. User updated: " + updatedUser.getUserUID());
                            // If the user has more than 200 credits, just return the updated user
                            return userEventPublisherService.publishUserUpdated(updatedUser)
                                    .thenReturn(updatedUser);
                        }
                        // Optionally, you can publish an event or perform additional actions here
                    }).onErrorResume(throwable -> {
                        // Handle any errors that occur during the process
                        if (step1.getRewards().getIsPremium()) {
                            return Mono
                                    .error(throwable);
                        }

                    else {
                            // If the user is not premium and an error occurs, we can return an error
                            // or handle it as needed.
                            // Here we just restore the user credits to the original state
                            System.out.println("Error processing purchase: " + throwable.getMessage());
                            // Restore the user's credits to the original state
                            // and we return the error to the user, not the updated user
                            return userModelRepository
                                    .updateProfileCredits(userUID, step1.getRewards().getCoins(),
                                            step1.getRewards().getNextDailyRewardTimestamp(),
                                            step1.getRewards().getWaitingReward())
                                    .flatMap(finalUser2 -> {

                                        // Return the original user state
                                        return Mono.just(finalUser2).doOnSuccess(
                                                data -> userEventPublisherService.publishUserUpdated(data).then());
                                    })
                                    .onErrorResume(updateError -> {
                                        System.out.println("Error restoring user credits: " + updateError.getMessage());
                                        return Mono.error(throwable);
                                    });
                        }
                    });

        });

    }

}
