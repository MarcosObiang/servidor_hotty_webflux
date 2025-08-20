package com.hotty.subscriptions_service.services;

import com.hotty.subscriptions_service.DTOs.WebhookEvent;
import com.hotty.subscriptions_service.model.UserSubscription;
import com.hotty.subscriptions_service.repository.UserSubscriptionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserSubscriptionService {

    private final UserSubscriptionRepository repository;

    private static final String SUBSCRIBED = "SUBSCRIBED";
    private static final String CANCELLATION = "CANCELLATION";
    private static final String PAYMENT_ISSUES = "PAYMENT_ISSUES";
    private static final String NOT_SUBSCRIBED = "NOT_SUBSCRIBED";

    /**
     * Procesa un evento de webhook de RevenueCat.
     * @param event El payload del evento de webhook.
     * @return Un Mono que se completa cuando el procesamiento ha terminado.
     */
    public Mono<Void> processWebhookEvent(WebhookEvent event) {
        // Ignorar eventos de prueba para no contaminar la base de datos
        if ("TEST".equals(event.getType())) {
            System.out.println("Evento de prueba recibido. No se realizarán acciones.");
            return Mono.empty();
        }

        String appUserId = event.getAppUserId();

        return repository.findById(appUserId)
                .flatMap(existing -> handleExistingSubscription(event, existing))
                .switchIfEmpty(Mono.defer(() -> handleNewSubscription(event)));
    }

    /**
     * Lógica para actualizar una suscripción existente.
     */
    private Mono<Void> handleExistingSubscription(WebhookEvent event, UserSubscription existing) {
        String eventType = event.getType();
        
        updateCommonFields(event, existing);

        switch (eventType) {
            case "INITIAL_PURCHASE":
            case "RENEWAL":
            case "UNCANCELLATION":
            case "PRODUCT_CHANGE":
                existing.setSubscriptionStatus(SUBSCRIBED);
                existing.setIsUserPremium(true);
                break;
            case "CANCELLATION":
                existing.setSubscriptionStatus(CANCELLATION);
                break;
            case "BILLING_ISSUE":
                existing.setSubscriptionStatus(PAYMENT_ISSUES);
                break;
            case "EXPIRATION":
                existing.setSubscriptionStatus(NOT_SUBSCRIBED);
                existing.setIsUserPremium(false);
                break;
            default:
                // Ignora eventos no manejados
                return Mono.empty();
        }

        // Usa `createUserSubscription` para actualizar el documento existente
        return repository.createUserSubscription(existing).then();
    }

    /**
     * Lógica para crear una nueva suscripción.
     */
    private Mono<Void> handleNewSubscription(WebhookEvent event) {
        // Solo crea una nueva suscripción si es la primera compra
        if ("INITIAL_PURCHASE".equals(event.getType())) {
            UserSubscription newSubscription = new UserSubscription();
            newSubscription.setId(event.getAppUserId());
            
            updateCommonFields(event, newSubscription);
            
            newSubscription.setSubscriptionStatus(SUBSCRIBED);
            newSubscription.setIsUserPremium(true);
            newSubscription.setOriginalTransactionId(event.getOriginalTransactionId());

            // Usa `createUserSubscription` para crear un nuevo documento
            return repository.createUserSubscription(newSubscription).then();
        }
        
        // Para cualquier otro evento (como RENEWAL) de un usuario que no existe, no hacemos nada
        return Mono.empty();
    }

    /**
     * Método auxiliar para actualizar los campos comunes del evento.
     */
    private void updateCommonFields(WebhookEvent event, UserSubscription subscription) {
        subscription.setSubscriptionId(event.getProductId());
        
        // Mapea el array de String a una List<String>
        List<String> entitlements = Optional.ofNullable(event.getEntitlementIds())
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        subscription.setEntitlementIds(entitlements);
        
        subscription.setExpirationAtMs(event.getExpirationAtMs());
        subscription.setExpiresDate(event.getExpiresDate());
        subscription.setStore(event.getStore());
        subscription.setIsTestAccount("SANDBOX".equals(event.getEnvironment()));
        subscription.setSubscriptionPausedAtMs(event.getSubscriptionPausedAtMs());
        subscription.setEndSubscriptionPauseAtMs(event.getEndSubscriptionPauseAtMs());
        subscription.setLastSeenAtMs(Instant.now().toEpochMilli());
    }
}