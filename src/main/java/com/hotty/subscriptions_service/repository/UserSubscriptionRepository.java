package com.hotty.subscriptions_service.repository;

import com.hotty.subscriptions_service.model.UserSubscription;

import reactor.core.publisher.Mono;

public interface UserSubscriptionRepository {

    /**
     * Creates a new user subscription document
     * @param userSubscription the user subscription to create
     * @return Mono of the created UserSubscription
     */
    Mono<UserSubscription> createUserSubscription(UserSubscription userSubscription);

    /**
     * Updates an entire user subscription document by ID and returns the updated document
     * @param id the ID of the document to update
     * @param userSubscription the updated user subscription data
     * @return Mono of the updated UserSubscription, or empty Mono if not found
     */
    Mono<UserSubscription> updateUserSubscriptionById(String id, UserSubscription userSubscription);

    /**
     * Updates an entire user subscription document by userId and returns the updated document
     * @param userId the userId of the document to update
     * @param userSubscription the updated user subscription data
     * @return Mono of the updated UserSubscription, or empty Mono if not found
     */
    Mono<UserSubscription> updateUserSubscriptionByUserId(String userId, UserSubscription userSubscription);

    /**
     * Finds a user subscription by ID
     * @param id the ID to search for
     * @return Mono of UserSubscription if found, empty Mono otherwise
     */
    Mono<UserSubscription> findById(String id);

    /**
     * Finds a user subscription by user ID
     * @param userId the user ID to search for
     * @return Mono of UserSubscription if found, empty Mono otherwise
     */
    Mono<UserSubscription> findByUserId(String userId);

    /**
     * Finds a user subscription by subscription ID
     * @param subscriptionId the subscription ID to search for
     * @return Mono of UserSubscription if found, empty Mono otherwise
     */
    Mono<UserSubscription> findBySubscriptionId(String subscriptionId);

    /**
     * Finds a user subscription by original transaction ID
     * @param originalTransactionId the original transaction ID to search for
     * @return Mono of UserSubscription if found, empty Mono otherwise
     */
    Mono<UserSubscription> findByOriginalTransactionId(String originalTransactionId);
}
