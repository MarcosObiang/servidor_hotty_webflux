package com.hotty.user_service.DTOs;

import lombok.Data;

import java.time.Instant;



/**
 * DTO para enviar actualizaciones de suscripción desde subscription service a
 * user service
 */

 @Data
public class UserSubscriptionUpdateDTO {

    private String userUID;
    private String appUserId;
    private String currentProductId;
    private String previousProductId; // Para eventos PRODUCT_CHANGE
    private Long expirationAtMs;
    private String store;
    private String originalTransactionId;
    private String environment;
    private String countryCode;
    private String currency;
    private Long lastEventTimestampMs;
    private Boolean isTestAccount;
    private String subscriptionStatus;
    private Boolean isUserPremium;
    private Instant updatedAt;
    private String eventType;
    private String[] transferFrom;
    private String[] transferTo;

    // Constructor vacío
    public UserSubscriptionUpdateDTO() {
    }

    // Constructor completo
    public UserSubscriptionUpdateDTO(String userUID, String appUserId, String currentProductId,
            Long expirationAtMs, String store, String originalTransactionId,
            String environment, String countryCode, String currency,
            Long lastEventTimestampMs, Boolean isTestAccount,
            String subscriptionStatus, Boolean isUserPremium,
            Instant updatedAt, String eventType) {
        this.userUID = userUID;
        this.appUserId = appUserId;
        this.currentProductId = currentProductId;
        this.expirationAtMs = expirationAtMs;
        this.store = store;
        this.originalTransactionId = originalTransactionId;
        this.environment = environment;
        this.countryCode = countryCode;
        this.currency = currency;
        this.lastEventTimestampMs = lastEventTimestampMs;
        this.isTestAccount = isTestAccount;
        this.subscriptionStatus = subscriptionStatus;
        this.isUserPremium = isUserPremium;
        this.updatedAt = updatedAt;
        this.eventType = eventType;
    }






}
