package com.hotty.user_service.usecases;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.subscriptions_service.DTOs.RevenueCatWebhookEvent;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserSubscription;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class ProcessRevenueCatPurchasesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessRevenueCatPurchasesUseCase.class);

    private final UserModelRepository userDataModelRepository;
    private final UserEventPublisherService userEventPublisherService;

    public ProcessRevenueCatPurchasesUseCase(UserModelRepository userDataModelRepository,
            UserEventPublisherService userEventPublisherService) {
        this.userDataModelRepository = userDataModelRepository;
        this.userEventPublisherService = userEventPublisherService;
    }

    /**
     * Procesa un webhook de RevenueCat y actualiza toda la información del usuario
     */
    public Mono<String> execute(RevenueCatWebhookEvent webhookEvent) {
        log.info("Processing RevenueCat webhook - User: {}, Event: {}, Product: {}",
                webhookEvent.getAppUserId(),
                webhookEvent.getType(),
                webhookEvent.getProductId());

        return validateWebhookEvent(webhookEvent)
                .then(findOrCreateUser(webhookEvent))
                .flatMap(user -> processSubscriptionUpdate(user, webhookEvent))
                .flatMap(userDataModelRepository::save)
                .flatMap(savedUser -> publishSubscriptionEvent(savedUser, webhookEvent))
                .map(user -> buildSuccessResponse(user, webhookEvent))
                .doOnSuccess(response -> log.info("RevenueCat webhook processed successfully: {}", response))
                .doOnError(error -> log.error("Failed to process RevenueCat webhook for user: {}",
                        webhookEvent.getAppUserId(), error));
    }

    /**
     * Valida que el webhook tenga los datos mínimos requeridos
     */
    private Mono<Void> validateWebhookEvent(RevenueCatWebhookEvent webhookEvent) {
        if (webhookEvent.getAppUserId() == null || webhookEvent.getAppUserId().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("WebhookEvent must have a valid app_user_id"));
        }

        if (webhookEvent.getType() == null || webhookEvent.getType().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("WebhookEvent must have a valid event type"));
        }

        log.debug("Webhook validation passed for user: {}", webhookEvent.getAppUserId());
        return Mono.empty();
    }

    /**
     * Busca el usuario existente o crea uno nuevo si no existe
     */
    private Mono<UserDataModel> findOrCreateUser(RevenueCatWebhookEvent webhookEvent) {
        return userDataModelRepository.findByUserUID(webhookEvent.getAppUserId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn(
                            "User not found for RevenueCat webhook: {}. This might indicate the user was deleted or the webhook is for a different environment.",
                            webhookEvent.getAppUserId());
                    return Mono.error(new RuntimeException("User not found: " + webhookEvent.getAppUserId()));
                }))
                .doOnSuccess(user -> log.debug("Found user for webhook processing: {}", user.getUserUID()));
    }

    /**
     * Procesa la actualización de suscripción basada en el webhook
     */
    private Mono<UserDataModel> processSubscriptionUpdate(UserDataModel user, RevenueCatWebhookEvent webhookEvent) {
        log.debug("Processing subscription update for user: {}", user.getUserUID());

        // Obtener o crear suscripción
        UserSubscription subscription = getOrCreateSubscription(user);

        // Mapear datos del webhook
        mapWebhookToSubscription(subscription, webhookEvent, user.getUserUID());

        // Determinar estado de suscripción
        updateSubscriptionStatus(subscription, webhookEvent);

        // Asignar suscripción actualizada
        user.setSubscription(subscription);

        log.info("Updated subscription for user: {} - Status: {}, Premium: {}",
                user.getUserUID(),
                subscription.getSubscriptionStatus(),
                subscription.getIsUserPremium());

        return Mono.just(user);
    }

    /**
     * Obtiene la suscripción existente o crea una nueva y la asigna al usuario
     */
    private UserSubscription getOrCreateSubscription(UserDataModel user) {
        UserSubscription subscription = user.getSubscription();
        if (subscription == null) {
            subscription = new UserSubscription();
            subscription.setCreatedAt(Instant.now());
            // ✅ ASIGNAR inmediatamente la nueva suscripción al usuario
            user.setSubscription(subscription);
            log.debug("Created and assigned new subscription for user: {}", user.getUserUID());
        } else {
            log.debug("Using existing subscription for user: {}", user.getUserUID());
        }
        return subscription;
    }

    /**
     * Mapea los datos del webhook a la suscripción
     */
    private void mapWebhookToSubscription(UserSubscription subscription, RevenueCatWebhookEvent webhookEvent, String userUID) {
        subscription.setAppUserId(webhookEvent.getAppUserId());
        subscription.setUserId(userUID);
        subscription.setCurrentProductId(webhookEvent.getProductId());
        subscription.setExpirationAtMs(webhookEvent.getExpirationAtMs());
        subscription.setStore(webhookEvent.getStore());
        subscription.setOriginalTransactionId(webhookEvent.getOriginalTransactionId());
        subscription.setEnvironment(webhookEvent.getEnvironment());
        subscription.setCountryCode(webhookEvent.getCountryCode());
        subscription.setCurrency(webhookEvent.getCurrency());
        subscription.setLastEventTimestampMs(webhookEvent.getEventTimestampMs());
        subscription.setIsTestAccount("SANDBOX".equalsIgnoreCase(webhookEvent.getEnvironment()));
        subscription.setUpdatedAt(Instant.now());

        log.debug("Mapped webhook data to subscription for user: {}", userUID);
    }

    /**
     * Actualiza el estado de la suscripción basado en el tipo de evento
     */
    private void updateSubscriptionStatus(UserSubscription subscription, RevenueCatWebhookEvent webhookEvent) {
        String eventType = webhookEvent.getType();
        Long expirationMs = webhookEvent.getExpirationAtMs();
        Instant now = Instant.now();
        Instant expirationDate = expirationMs != null ? Instant.ofEpochMilli(expirationMs) : null;

        switch (eventType) {
            case "INITIAL_PURCHASE":
                handleInitialPurchase(subscription, expirationDate, now);
                break;

            case "RENEWAL":
            case "PRODUCT_CHANGE":
                handleRenewalOrChange(subscription, expirationDate, now);
                break;

            case "CANCELLATION":
                handleCancellation(subscription, expirationDate, now);
                break;

            case "EXPIRATION":
            case "BILLING_ISSUE":
                handleExpiration(subscription);
                break;

            case "SUBSCRIBER_ALIAS":
                handleSubscriberAlias(subscription);
                break;

            default:
                handleUnknownEvent(subscription, eventType, expirationDate, now, webhookEvent.getAppUserId());
                break;
        }

        log.debug("Updated subscription status for user: {} - Status: {}, Premium: {}",
                webhookEvent.getAppUserId(),
                subscription.getSubscriptionStatus(),
                subscription.getIsUserPremium());
    }

    private void handleInitialPurchase(UserSubscription subscription, Instant expirationDate, Instant now) {
        subscription.setSubscriptionStatus("ACTIVE");
        if (expirationDate != null && expirationDate.isAfter(now)) {
            subscription.setIsUserPremium(true);
        } else {
            subscription.setIsUserPremium(false);
            subscription.setSubscriptionStatus("EXPIRED");
        }
    }

    private void handleRenewalOrChange(UserSubscription subscription, Instant expirationDate, Instant now) {
        subscription.setSubscriptionStatus("ACTIVE");
        if (expirationDate != null && expirationDate.isAfter(now)) {
            subscription.setIsUserPremium(true);
        } else {
            subscription.setIsUserPremium(false);
            subscription.setSubscriptionStatus("EXPIRED");
        }
    }

    private void handleCancellation(UserSubscription subscription, Instant expirationDate, Instant now) {
        subscription.setSubscriptionStatus("CANCELLED");
        // El usuario sigue siendo premium hasta la fecha de expiración
        if (expirationDate != null && expirationDate.isAfter(now)) {
            subscription.setIsUserPremium(true);
        } else {
            subscription.setIsUserPremium(false);
        }
    }

    private void handleExpiration(UserSubscription subscription) {
        subscription.setSubscriptionStatus("EXPIRED");
        subscription.setIsUserPremium(false);
    }

    private void handleSubscriberAlias(UserSubscription subscription) {
        // Para SUBSCRIBER_ALIAS, solo actualizamos los datos pero no el estado
        log.debug("Processing SUBSCRIBER_ALIAS event - updating data only");
    }

    private void handleUnknownEvent(UserSubscription subscription, String eventType, Instant expirationDate,
            Instant now, String userId) {
        log.warn("Unknown RevenueCat event type: {} for user: {}", eventType, userId);

        // Fallback: determinar estado basado en fecha de expiración
        if (expirationDate != null && expirationDate.isAfter(now)) {
            subscription.setIsUserPremium(true);
            subscription.setSubscriptionStatus("ACTIVE");
        } else {
            subscription.setIsUserPremium(false);
            subscription.setSubscriptionStatus("EXPIRED");
        }
    }

    /**
     * Publica el evento correspondiente según el tipo de webhook
     */
    private Mono<UserDataModel> publishSubscriptionEvent(UserDataModel user, RevenueCatWebhookEvent webhookEvent) {
        String eventType = webhookEvent.getType();
        UserSubscription subscription = user.getSubscription();

        Mono<Void> publishEvent = determineEventToPublish(user);

        return publishEvent
                .thenReturn(user)
                .onErrorResume(error -> {
                    log.warn("Failed to publish subscription event for user: {}, but continuing processing: {}",
                            user.getUserUID(), error.getMessage());
                    return Mono.just(user);
                });
    }

    private Mono<Void> determineEventToPublish(UserDataModel user) {
        return userEventPublisherService.publishUserUpdated(user);

    }

    /**
     * Construye la respuesta de éxito
     */
    private String buildSuccessResponse(UserDataModel user, RevenueCatWebhookEvent webhookEvent) {
        UserSubscription subscription = user.getSubscription();
        return String.format("RevenueCat webhook processed successfully for user %s: Event=%s, Status=%s, Premium=%s",
                user.getUserUID(),
                webhookEvent.getType(),
                subscription.getSubscriptionStatus(),
                subscription.getIsUserPremium());
    }
}
