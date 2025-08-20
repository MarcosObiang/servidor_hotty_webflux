
package com.hotty.subscriptions_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.hotty.subscriptions_service.DTOs.WebhookEvent;
import com.hotty.subscriptions_service.DTOs.WebhookPayload;
import com.hotty.subscriptions_service.model.UserSubscription;
import com.hotty.subscriptions_service.services.UserSubscriptionService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionsController {

    private UserSubscriptionService userSubscriptionService;

    public SubscriptionsController(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
    }

    @PostMapping("/webhooks/revenuecat")
    public Mono<ResponseEntity<Object>> webhook(@RequestBody WebhookPayload event) {
        // Redirecciona la solicitud al servicio de backend.
        // La validación de la firma se realiza en el filtro del Gateway antes de llegar
        // aquí.
     return    userSubscriptionService.processWebhookEvent(event.getEvent()).doOnSuccess(data -> {
         System.out.println("Evento procesado correctamente: " + event);
     }).doOnError(error -> {
         System.err.println("Error procesando el evento: " + error.getMessage());
     }).then(Mono.just(ResponseEntity.ok().build()));
    }

}
