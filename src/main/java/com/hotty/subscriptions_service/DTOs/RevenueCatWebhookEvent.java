package com.hotty.subscriptions_service.DTOs;


import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@Data
public class RevenueCatWebhookEvent {
    
    // Identificadores de la transacción y el usuario
    @JsonProperty("app_user_id")
    private String appUserId;
    
    @JsonProperty("original_app_user_id")
    private String originalAppUserId;

    @JsonProperty("original_transaction_id")
    private String originalTransactionId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("aliases")
    private String[] aliases;

    // Información de la suscripción
    @JsonProperty("type")
    private String type; // Ej: INITIAL_PURCHASE, RENEWAL, CANCELLATION, etc.

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("new_product_id")
    private String newProductId; // Para eventos PRODUCT_CHANGE - el nuevo producto

    @JsonProperty("entitlement_ids")
    private String[] entitlementIds;

    @JsonProperty("store")
    private String store; // Ej: APP_STORE, PLAY_STORE, STRIPE

    @JsonProperty("period_type")
    private String periodType; // Ej: NORMAL, TRIAL, INTRO

    @JsonProperty("environment")
    private String environment; // Ej: SANDBOX, PRODUCTION

    // Fechas y timestamps
    @JsonProperty("event_timestamp_ms")
    private long eventTimestampMs;

    @JsonProperty("purchased_at_ms")
    private Long purchasedAtMs; // Puede ser null

    @JsonProperty("expiration_at_ms")
    private long expirationAtMs;

    @JsonProperty("expires_date")
    private String expiresDate;
    
    // Campos de estado
    @JsonProperty("is_test")
    private Boolean isTest; // Nuevo en API v2

    @JsonProperty("is_trial_period")
    private Boolean isTrialPeriod; // Puede ser null

    @JsonProperty("is_family_share")
    private Boolean isFamilyShare; // Puede ser null
    
    // Campos de precios
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("price_in_purchased_currency")
    private Double priceInPurchasedCurrency;

    @JsonProperty("currency")
    private String currency;
    
    // Atributos de suscriptor (si los usas)
    @JsonProperty("subscriber_attributes")
    private Map<String, Object> subscriberAttributes;

    // Otros campos
    @JsonProperty("country_code")
    private String countryCode;

    // Campos de pausa de suscripción (pueden ser null)
    @JsonProperty("subscription_paused_at_ms")
    private Long subscriptionPausedAtMs;

    @JsonProperty("end_subscription_pause_at_ms")
    private Long endSubscriptionPauseAtMs;

    // TRANSFER - Solo presente en eventos TRANSFER
    @JsonProperty("transferred_from")
    private String[] transferredFrom;

    @JsonProperty("transferred_to")
    private String[] transferredTo;

    // NON_RENEWING_PURCHASE - Campos específicos para compras consumibles
    @JsonProperty("entitlement_id")
    private String entitlementId; // ID del entitlement individual (null para consumibles)

    @JsonProperty("presented_offering_id")
    private String presentedOfferingId; // ID de la oferta presentada (ej: "coins")

    @JsonProperty("takehome_percentage")
    private Double takehomePercentage; // Porcentaje que se queda la app (ej: 0.85)

    @JsonProperty("tax_percentage")
    private Double taxPercentage; // Porcentaje de impuestos (ej: 0.0)

    @JsonProperty("commission_percentage")
    private Double commissionPercentage; // Porcentaje de comisión de la tienda (ej: 0.15)

    @JsonProperty("offer_code")
    private String offerCode; // Código de oferta aplicado (puede ser null)

    @JsonProperty("id")
    private String id; // ID único del evento

    @JsonProperty("app_id")
    private String appId; // ID de la aplicación

    @JsonProperty("experiments")
    private Experiment[] experiments; // Experimentos A/B asociados

    /**
     * Método para obtener el appUserId apropiado según el tipo de evento.
     * Para eventos TRANSFER, utiliza transferred_to[0] como el usuario destino.
     * Para otros eventos, utiliza app_user_id normal.
     */
    public String getEffectiveAppUserId() {
        if ("TRANSFER".equals(type) && transferredTo != null && transferredTo.length > 0) {
            return transferredTo[0];
        }
        return appUserId;
    }

    /**
     * Clase interna para representar experimentos A/B de RevenueCat
     */
    @Data
    public static class Experiment {
        @JsonProperty("experiment_id")
        private String experimentId;
        
        @JsonProperty("experiment_variant")
        private String experimentVariant;
    }
}