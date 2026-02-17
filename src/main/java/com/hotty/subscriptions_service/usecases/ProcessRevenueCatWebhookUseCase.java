package com.hotty.subscriptions_service.usecases;

import com.hotty.subscriptions_service.DTOs.RevenueCatWebhookEvent;
import com.hotty.subscriptions_service.services.UserSubscriptionNotificationService;
import com.hotty.user_service.DTOs.UserSubscriptionUpdateDTO;
import com.hotty.user_service.model.UserSubscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Component
public class ProcessRevenueCatWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessRevenueCatWebhookUseCase.class);
    
    private final UserSubscriptionNotificationService notificationService;

    public ProcessRevenueCatWebhookUseCase(UserSubscriptionNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Procesa el webhook de RevenueCat, genera DTOs y los envía al stream de notificaciones
     * Para NON_RENEWING_PURCHASE procesa y notifica
     * Para PRODUCT_CHANGE NO se emite ningún evento (early return)
     */
    public Flux<UserSubscriptionUpdateDTO> execute(RevenueCatWebhookEvent webhookEvent) {
        String eventType = webhookEvent.getType();
        log.info("Processing RevenueCat webhook event: {} for user: {}", 
            eventType, webhookEvent.getEffectiveAppUserId());

        // Si es PRODUCT_CHANGE, terminar temprano sin emitir eventos
        if ("PRODUCT_CHANGE".equals(eventType)) {
            log.warn("🚫 PRODUCT_CHANGE detected - EARLY RETURN WITHOUT PROCESSING for user: {}", 
                webhookEvent.getEffectiveAppUserId());
            return Flux.empty();
        }

        // Si es NON_RENEWING_PURCHASE, procesarlo y notificar
        if ("NON_RENEWING_PURCHASE".equals(eventType)) {
            log.info("NON_RENEWING_PURCHASE event received - WILL process and notify for user: {}", 
                webhookEvent.getEffectiveAppUserId());
        }

        return validateWebhookEvent(webhookEvent)
                .thenMany(processWebhookEvent(webhookEvent))
                .doOnNext(dto -> {
                    // Enviar cada actualización al stream de notificaciones
                    notificationService.sendSubscriptionUpdate(dto);
                    log.info("RevenueCat webhook processed and notification sent: {}", dto);
                })
                .doOnError(error -> log.error("Failed to process RevenueCat webhook for user {}: {}", 
                    webhookEvent.getEffectiveAppUserId(), error.getMessage()));
    }

    /**
     * Valida los datos del webhook
     */
    private Mono<Void> validateWebhookEvent(RevenueCatWebhookEvent webhookEvent) {
        if (webhookEvent == null) {
            return Mono.error(new IllegalArgumentException("WebhookEvent cannot be null"));
        }

        // Para eventos TRANSFER, usar el método getEffectiveAppUserId()
        String effectiveUserId = webhookEvent.getEffectiveAppUserId();
        if (effectiveUserId == null || effectiveUserId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("AppUserId is required"));
        }

        if (webhookEvent.getType() == null || webhookEvent.getType().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Event type is required"));
        }

        log.debug("WebhookEvent validation passed for user: {}", effectiveUserId);
        return Mono.empty();
    }

    /**
     * Procesa el evento de webhook y crea los DTOs de actualización
     * Para TRANSFER genera 2 DTOs (origen pierde premium, destino gana premium)
     * Para otros eventos genera 1 solo DTO
     */
    private Flux<UserSubscriptionUpdateDTO> processWebhookEvent(RevenueCatWebhookEvent webhookEvent) {
        String eventType = webhookEvent.getType();
        
        if ("TRANSFER".equals(eventType)) {
            return handleTransferEvent(webhookEvent);
        } else {
            // Para eventos regulares, devolver un solo DTO
            return handleRegularEvent(webhookEvent);
        }
    }

    /**
     * Maneja eventos regulares (no TRANSFER)
     */
    private Flux<UserSubscriptionUpdateDTO> handleRegularEvent(RevenueCatWebhookEvent webhookEvent) {
        // Crear una nueva suscripción temporal para procesar la lógica
        UserSubscription subscription = new UserSubscription();
        
        // Mapear datos básicos del webhook
        mapWebhookToSubscription(subscription, webhookEvent);
        
        // Actualizar el estado de la suscripción basado en el tipo de evento
        updateSubscriptionStatus(subscription, webhookEvent);
        
        // Crear el DTO de actualización
        UserSubscriptionUpdateDTO dto = createUpdateDTO(subscription, webhookEvent);
        
        return Flux.just(dto);
    }

    /**
     * Maneja eventos TRANSFER - genera 2 DTOs idénticos excepto por app_user_id y estado premium
     */
    private Flux<UserSubscriptionUpdateDTO> handleTransferEvent(RevenueCatWebhookEvent webhookEvent) {
        log.info("Processing TRANSFER event - generating DTOs for source and destination users");
        
        String[] transferredFrom = webhookEvent.getTransferredFrom();
        String destinationUserId = webhookEvent.getEffectiveAppUserId(); // Usuario destino del transfer
        
        if (transferredFrom == null || transferredFrom.length == 0) {
            log.warn("Transfer event received but no transferred_from users found, processing as regular event");
            return handleRegularEvent(webhookEvent);
        }
        
        // DTO para el usuario destino (gana premium)
        UserSubscriptionUpdateDTO destinationDto = createTransferDTO(webhookEvent, destinationUserId, true, "ACTIVE");
        
        // DTOs para los usuarios origen (pierden premium) 
        Flux<UserSubscriptionUpdateDTO> sourceUsersFlux = Flux.fromArray(transferredFrom)
                .map(sourceUserId -> {
                    log.info("Creating transfer DTO for source user: {} - losing premium", sourceUserId);
                    return createTransferDTO(webhookEvent, sourceUserId, false, "TRANSFERRED");
                });
        
        // Combinar destino + orígenes
        return Flux.concat(Flux.just(destinationDto), sourceUsersFlux);
    }

    /**
     * Mapea los datos del webhook a la suscripción
     */
    private void mapWebhookToSubscription(UserSubscription subscription, RevenueCatWebhookEvent webhookEvent) {
        String effectiveUserId = webhookEvent.getEffectiveAppUserId();
        subscription.setAppUserId(effectiveUserId);
        subscription.setUserId(effectiveUserId); // En este contexto, userUID = appUserId
        
        // Para PRODUCT_CHANGE NO actualizamos el producto porque es solo informativo
        // El cambio real se aplicará en el siguiente RENEWAL o INITIAL_PURCHASE
        if (!"PRODUCT_CHANGE".equals(webhookEvent.getType())) {
            subscription.setCurrentProductId(webhookEvent.getProductId());
        }
        // Si es PRODUCT_CHANGE, mantenemos el producto actual sin cambios
        
        subscription.setExpirationAtMs(webhookEvent.getExpirationAtMs());
        subscription.setStore(webhookEvent.getStore());
        subscription.setOriginalTransactionId(webhookEvent.getOriginalTransactionId());
        subscription.setEnvironment(webhookEvent.getEnvironment());
        subscription.setCountryCode(webhookEvent.getCountryCode());
        subscription.setCurrency(webhookEvent.getCurrency());
        subscription.setLastEventTimestampMs(webhookEvent.getEventTimestampMs());
        subscription.setIsTestAccount("SANDBOX".equalsIgnoreCase(webhookEvent.getEnvironment()));
        subscription.setUpdatedAt(Instant.now());

        log.debug("Mapped webhook data to subscription for user: {}", webhookEvent.getEffectiveAppUserId());
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
                handleRenewal(subscription, expirationDate, now);
                break;

            case "NON_RENEWING_PURCHASE":
                handleNonRenewingPurchase(subscription, webhookEvent);
                break;

            case "PRODUCT_CHANGE":
                handleProductChange(subscription, expirationDate, now, webhookEvent);
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
                webhookEvent.getEffectiveAppUserId(),
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

    private void handleRenewal(UserSubscription subscription, Instant expirationDate, Instant now) {
        subscription.setSubscriptionStatus("ACTIVE");
        if (expirationDate != null && expirationDate.isAfter(now)) {
            subscription.setIsUserPremium(true);
        } else {
            subscription.setIsUserPremium(false);
            subscription.setSubscriptionStatus("EXPIRED");
        }
        log.debug("Processed RENEWAL - subscription renewed and active");
    }

    private void handleNonRenewingPurchase(UserSubscription subscription, RevenueCatWebhookEvent webhookEvent) {
        // NON_RENEWING_PURCHASE es para compras consumibles (coins, tokens, etc.)
        // Estas compras NO otorgan premium ya que son productos consumibles
        subscription.setSubscriptionStatus("NON_RENEWING_PURCHASE");
        subscription.setIsUserPremium(false); // Los consumibles no otorgan premium
        
        String productId = webhookEvent.getProductId();
        String presentedOffering = webhookEvent.getPresentedOfferingId();
        Double price = webhookEvent.getPrice();
        String currency = webhookEvent.getCurrency();
        
        log.info("NON_RENEWING_PURCHASE processed for user: {} - Product: {}, Offering: {}, Price: {} {}", 
                webhookEvent.getEffectiveAppUserId(), 
                productId != null ? productId : "unknown",
                presentedOffering != null ? presentedOffering : "unknown",
                price != null ? price : "unknown", 
                currency != null ? currency : "unknown");
        
        // Log información adicional para debugging
        if (webhookEvent.getTakehomePercentage() != null) {
            log.debug("Revenue details - Takehome: {}%, Commission: {}%, Tax: {}%", 
                    webhookEvent.getTakehomePercentage() * 100,
                    webhookEvent.getCommissionPercentage() != null ? webhookEvent.getCommissionPercentage() * 100 : 0,
                    webhookEvent.getTaxPercentage() != null ? webhookEvent.getTaxPercentage() * 100 : 0);
        }
        
        log.debug("Processed NON_RENEWING_PURCHASE - consumible purchased, no premium granted");
    }

    private void handleProductChange(UserSubscription subscription, Instant expirationDate, Instant now, RevenueCatWebhookEvent webhookEvent) {
        // PRODUCT_CHANGE es solo INFORMATIVO según RevenueCat:
        // - expiration_at_ms es del producto ANTERIOR (del que está cambiando)
        // - El cambio aún NO ha entrado en efecto
        // - El cambio real se aplicará en el siguiente RENEWAL (Apple/Stripe) o INITIAL_PURCHASE (Google Play)
        
        // Obtener información del producto anterior y nuevo
        String previousProduct = webhookEvent.getProductId(); // En PRODUCT_CHANGE, product_id es el anterior
        String newProduct = webhookEvent.getNewProductId(); // El nuevo producto
        
        // IMPORTANTE: NO actualizamos NADA en la suscripción porque el cambio aún no es efectivo
        // Solo mantenemos el producto actual hasta que llegue el evento que confirme el cambio
        
        // Log informativo del cambio pendiente
        log.info("PRODUCT_CHANGE (INFORMATIVE) for user: {} - Scheduled change from '{}' to '{}', Store: {}, Current expiration (old product): {}", 
                webhookEvent.getEffectiveAppUserId(), 
                previousProduct != null ? previousProduct : "unknown",
                newProduct != null ? newProduct : "unknown",
                webhookEvent.getStore(),
                expirationDate != null ? expirationDate : "unknown");
        
        // Detectar tipo de cambio programado
        if (previousProduct != null && newProduct != null) {
            if (previousProduct.contains("month") && newProduct.contains("year")) {
                log.info("Scheduled upgrade from monthly to yearly plan - will take effect on next RENEWAL");
            } else if (previousProduct.contains("year") && newProduct.contains("month")) {
                log.info("Scheduled downgrade from yearly to monthly plan - will take effect on next RENEWAL");
            } else if (previousProduct.contains("basic") && newProduct.contains("premium")) {
                log.info("Scheduled upgrade from basic to premium plan - will take effect on next RENEWAL");
            } else if (previousProduct.contains("premium") && newProduct.contains("basic")) {
                log.info("Scheduled downgrade from premium to basic plan - will take effect on next RENEWAL");
            }
        }
        
        // Información del precio es para el nuevo producto pero aún no aplicado
        if (webhookEvent.getPrice() != null) {
            log.info("New product '{}' will have price: {} {} (not yet effective)", 
                    newProduct, webhookEvent.getPrice(), webhookEvent.getCurrency());
        }
        
        // NOTA CRÍTICA: No modificamos subscription.currentProductId ni ningún otro campo
        // El producto y todas las condiciones permanecen iguales hasta el RENEWAL/INITIAL_PURCHASE
        log.info("PRODUCT_CHANGE is informative only - user keeps current product '{}' and premium status until next billing event", 
                subscription.getCurrentProductId());
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
        // Para SUBSCRIBER_ALIAS, mantenemos el estado actual pero actualizamos datos
        // Como no tenemos estado previo, asumimos que es información adicional
        log.debug("Processing SUBSCRIBER_ALIAS event - updating data only");
        // Por defecto, no cambiamos el estado premium, solo actualizamos información
        subscription.setSubscriptionStatus("ACTIVE"); // Estado por defecto para alias
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
     * Crea el DTO de actualización desde la suscripción procesada
     */
    private UserSubscriptionUpdateDTO createUpdateDTO(UserSubscription subscription, RevenueCatWebhookEvent webhookEvent) {
        UserSubscriptionUpdateDTO dto = new UserSubscriptionUpdateDTO();
        
        // Datos de identificación
        dto.setUserUID(webhookEvent.getEffectiveAppUserId());
        dto.setAppUserId(subscription.getAppUserId());
        
        // Datos de suscripción
        dto.setCurrentProductId(subscription.getCurrentProductId());
        
        // Para PRODUCT_CHANGE (informativo), incluir información sobre el cambio programado
        if ("PRODUCT_CHANGE".equals(webhookEvent.getType())) {
            String currentProduct = webhookEvent.getProductId(); // product_id es el producto actual en PRODUCT_CHANGE
            String scheduledProduct = webhookEvent.getNewProductId(); // new_product_id es el programado
            
            // Marcamos que hay un cambio programado pero no aplicado aún
            if (currentProduct != null && !currentProduct.trim().isEmpty()) {
                dto.setPreviousProductId("SCHEDULED_CHANGE_FROM_" + currentProduct); 
            } else {
                dto.setPreviousProductId("PRODUCT_CHANGE_SCHEDULED"); 
            }
            
            log.debug("PRODUCT_CHANGE (informative) in DTO for user: {} - Current: {}, Scheduled: {}", 
                    webhookEvent.getEffectiveAppUserId(), currentProduct, scheduledProduct);
        }
        
        dto.setExpirationAtMs(subscription.getExpirationAtMs());
        dto.setStore(subscription.getStore());
        dto.setOriginalTransactionId(subscription.getOriginalTransactionId());
        dto.setEnvironment(subscription.getEnvironment());
        dto.setCountryCode(subscription.getCountryCode());
        dto.setCurrency(subscription.getCurrency());
        dto.setLastEventTimestampMs(subscription.getLastEventTimestampMs());
        dto.setIsTestAccount(subscription.getIsTestAccount());
        
        // Estado de suscripción
        dto.setSubscriptionStatus(subscription.getSubscriptionStatus());
        dto.setIsUserPremium(subscription.getIsUserPremium());
        dto.setUpdatedAt(subscription.getUpdatedAt());
        
        // Información del evento
        dto.setEventType(webhookEvent.getType());
        
        return dto;
    }

    /**
     * Crea un DTO específico para eventos TRANSFER con datos idénticos excepto userId y estado premium
     */
    private UserSubscriptionUpdateDTO createTransferDTO(RevenueCatWebhookEvent webhookEvent, String userId, 
                                                       boolean isPremium, String subscriptionStatus) {
        UserSubscriptionUpdateDTO dto = new UserSubscriptionUpdateDTO();
        
        // Datos de identificación (lo único que cambia entre DTOs de transfer)
        dto.setUserUID(userId);
        dto.setAppUserId(userId);
        
        // Datos de suscripción (idénticos en todos los DTOs de transfer)
        dto.setCurrentProductId(webhookEvent.getProductId());
        dto.setPreviousProductId(null); // Los TRANSFER no incluyen cambio de producto
        dto.setExpirationAtMs(webhookEvent.getExpirationAtMs());
        dto.setStore(webhookEvent.getStore());
        dto.setOriginalTransactionId(webhookEvent.getOriginalTransactionId());
        dto.setEnvironment(webhookEvent.getEnvironment());
        dto.setCountryCode(webhookEvent.getCountryCode());
        dto.setCurrency(webhookEvent.getCurrency());
        dto.setLastEventTimestampMs(webhookEvent.getEventTimestampMs());
        dto.setIsTestAccount("SANDBOX".equalsIgnoreCase(webhookEvent.getEnvironment()));
        
        // Estado de suscripción (diferente para origen vs destino)
        dto.setSubscriptionStatus(subscriptionStatus);
        dto.setIsUserPremium(isPremium);
        dto.setUpdatedAt(Instant.now());
        
        // Información del evento (siempre TRANSFER)
        dto.setEventType("TRANSFER");
        
        log.debug("Created transfer DTO for user: {} - Premium: {} - Status: {}", 
                 userId, isPremium, subscriptionStatus);
        
        return dto;
    }
}
