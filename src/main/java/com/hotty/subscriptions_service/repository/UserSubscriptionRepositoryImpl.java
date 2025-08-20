package com.hotty.subscriptions_service.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Repository;

import com.hotty.subscriptions_service.model.UserSubscription;

import reactor.core.publisher.Mono;

@Repository
public class UserSubscriptionRepositoryImpl implements UserSubscriptionRepository {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Mono<UserSubscription> createUserSubscription(UserSubscription userSubscription) {
        return reactiveMongoTemplate.save(userSubscription);
    }

    @Override
    public Mono<UserSubscription> updateUserSubscriptionById(String id, UserSubscription userSubscription) {
        Query query = new Query(Criteria.where("id").is(id));
        
        Update update = new Update()
                .set("userId", userSubscription.getUserId())
                .set("isUserPremium", userSubscription.getIsUserPremium())
                .set("subscriptionStatus", userSubscription.getSubscriptionStatus())
                .set("subscriptionId", userSubscription.getSubscriptionId())
                .set("entitlement", userSubscription.getEntitlement())
                .set("expirationAtMs", userSubscription.getExpirationAtMs())
                .set("expiresDate", userSubscription.getExpiresDate())
                .set("store", userSubscription.getStore())
                .set("originalTransactionId", userSubscription.getOriginalTransactionId())
                .set("isTestAccount", userSubscription.getIsTestAccount())
                .set("subscriptionPausedAtMs", userSubscription.getSubscriptionPausedAtMs())
                .set("endSubscriptionPauseAtMs", userSubscription.getEndSubscriptionPauseAtMs())
                .set("lastSeenAtMs", userSubscription.getLastSeenAtMs());

        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        
        return reactiveMongoTemplate.findAndModify(query, update, options, UserSubscription.class);
    }

    @Override
    public Mono<UserSubscription> updateUserSubscriptionByUserId(String userId, UserSubscription userSubscription) {
        Query query = new Query(Criteria.where("userId").is(userId));
        
        Update update = new Update()
                .set("isUserPremium", userSubscription.getIsUserPremium())
                .set("subscriptionStatus", userSubscription.getSubscriptionStatus())
                .set("subscriptionId", userSubscription.getSubscriptionId())
                .set("entitlement", userSubscription.getEntitlement())
                .set("expirationAtMs", userSubscription.getExpirationAtMs())
                .set("expiresDate", userSubscription.getExpiresDate())
                .set("store", userSubscription.getStore())
                .set("originalTransactionId", userSubscription.getOriginalTransactionId())
                .set("isTestAccount", userSubscription.getIsTestAccount())
                .set("subscriptionPausedAtMs", userSubscription.getSubscriptionPausedAtMs())
                .set("endSubscriptionPauseAtMs", userSubscription.getEndSubscriptionPauseAtMs())
                .set("lastSeenAtMs", userSubscription.getLastSeenAtMs());

        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        
        return reactiveMongoTemplate.findAndModify(query, update, options, UserSubscription.class);
    }

    @Override
    public Mono<UserSubscription> findById(String id) {
        return reactiveMongoTemplate.findById(id, UserSubscription.class);
    }

    @Override
    public Mono<UserSubscription> findByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return reactiveMongoTemplate.findOne(query, UserSubscription.class);
    }

    @Override
    public Mono<UserSubscription> findBySubscriptionId(String subscriptionId) {
        Query query = new Query(Criteria.where("subscriptionId").is(subscriptionId));
        return reactiveMongoTemplate.findOne(query, UserSubscription.class);
    }

    @Override
    public Mono<UserSubscription> findByOriginalTransactionId(String originalTransactionId) {
        Query query = new Query(Criteria.where("originalTransactionId").is(originalTransactionId));
        return reactiveMongoTemplate.findOne(query, UserSubscription.class);
    }
}
