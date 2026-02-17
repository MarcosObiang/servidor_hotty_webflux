
package com.hotty.subscriptions_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotty.subscriptions_service.DTOs.RevenueCatWebhookPayload;
import com.hotty.subscriptions_service.services.SubscriptionWebhookService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/subscriptions-service")
public class SubscriptionsController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionsController.class);
    
    private final SubscriptionWebhookService subscriptionWebhookService;

    public SubscriptionsController(SubscriptionWebhookService subscriptionWebhookService) {
        this.subscriptionWebhookService = subscriptionWebhookService;
    }

    @PostMapping("/webhooks/revenuecat")
    public Mono<ResponseEntity<Object>> webhook(@RequestBody RevenueCatWebhookPayload webhookPayload) {
        log.info("Received RevenueCat webhook event: {}", webhookPayload.getEvent().getType());
        
        // Procesar el webhook y enviar actualización al user service
        return subscriptionWebhookService.processWebhookEvent(webhookPayload.getEvent())
                .doOnSuccess(result -> log.info("Webhook processed and notification sent successfully"))
                .doOnError(error -> log.error("Error processing webhook: {}", error.getMessage()))
                .then(Mono.just(ResponseEntity.ok().build()))
                .onErrorReturn(ResponseEntity.status(500).build());
    }
}
