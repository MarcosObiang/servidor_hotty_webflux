package com.hotty.subscriptions_service.services;

import com.hotty.subscriptions_service.DTOs.RevenueCatWebhookEvent;
import com.hotty.subscriptions_service.usecases.ProcessRevenueCatWebhookUseCase;
import com.hotty.user_service.DTOs.UserSubscriptionUpdateDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SubscriptionWebhookService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionWebhookService.class);
    
    private final ProcessRevenueCatWebhookUseCase processRevenueCatWebhookUseCase;
    private final UserSubscriptionNotificationService userSubscriptionNotificationService;

    public SubscriptionWebhookService(ProcessRevenueCatWebhookUseCase processRevenueCatWebhookUseCase,
                                    UserSubscriptionNotificationService userSubscriptionNotificationService) {
        this.processRevenueCatWebhookUseCase = processRevenueCatWebhookUseCase;
        this.userSubscriptionNotificationService = userSubscriptionNotificationService;
    }

    /**
     * Procesa un evento de webhook de RevenueCat y notifica al user service
     * Maneja tanto eventos regulares como transfers que pueden generar múltiples DTOs
     */
    public Mono<Void> processWebhookEvent(RevenueCatWebhookEvent webhookEvent) {
        log.info("Processing webhook event: {} for user: {}", 
            webhookEvent.getType(), webhookEvent.getAppUserId());

        return processRevenueCatWebhookUseCase.execute(webhookEvent)
                .flatMap(this::sendUpdateToUserService)
                .then()
                .doOnSuccess(aVoid -> log.info("Webhook event processed and notification sent for user: {}", 
                    webhookEvent.getAppUserId()))
                .doOnError(error -> log.error("Failed to process webhook event for user {}: {}", 
                    webhookEvent.getAppUserId(), error.getMessage()));
    }

    /**
     * Envía la actualización al user service
     */
    private Mono<Void> sendUpdateToUserService(UserSubscriptionUpdateDTO updateDTO) {
        log.debug("Sending subscription update to user service: {}", updateDTO);
        
        return userSubscriptionNotificationService.sendSubscriptionUpdate(updateDTO)
                .doOnSuccess(aVoid -> log.debug("Subscription update sent successfully"))
                .doOnError(error -> log.warn("Failed to send subscription update, but webhook processing continues: {}", 
                    error.getMessage()));
    }
}
