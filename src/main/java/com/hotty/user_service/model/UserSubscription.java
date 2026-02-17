package com.hotty.user_service.model;

import lombok.Data;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.hotty.user_service.DTOs.UserSubscriptionUpdateDTO;

import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Representa el estado actual de suscripción de un usuario.
 * Esta clase se actualiza basándose en los eventos de RevenueCat webhook.
 */
@Data
@Document(collection = "userSubscriptions")
public class UserSubscription {

    @Id
    private String appUserId; // app_user_id de RevenueCat
    
    @Indexed
    private String userId; // ID interno del usuario en tu app (si tienes mapping)
    
    // Estado calculado basado en eventos
    private Boolean isUserPremium;
    private String subscriptionStatus; // ACTIVE, EXPIRED, CANCELLED, PAUSED, etc.
    
    // Datos actuales de la suscripción
    private String currentProductId; // product_id activo
    private List<String> entitlementIds; // entitlement_ids activos
    private Long expirationAtMs; // expiration_at_ms de la suscripción activa
    private String store; // APP_STORE, PLAY_STORE, etc.
    private String originalTransactionId; // Para tracking de renovaciones
    
    // Metadatos
    private String environment; // PRODUCTION, SANDBOX
    private Boolean isTestAccount; // Calculado de environment
    private String countryCode; // Del último evento
    private String currency; // Del último evento
    
    // Timestamps de control
    private Long lastEventTimestampMs; // Último evento procesado
    private Instant updatedAt; // Cuándo se actualizó este registro
    private Instant createdAt; // Cuándo se creó la suscripción
    
    // Estados adicionales para funcionalidades avanzadas
    private Long subscriptionPausedAtMs; // Si está pausada
    private Long endSubscriptionPauseAtMs; // Cuándo termina la pausa
    
    /**
     * Calcula si el usuario tiene suscripción activa
     */
    public boolean isActiveSubscription() {
        if (expirationAtMs == null) return false;
        return System.currentTimeMillis() < expirationAtMs && 
               "ACTIVE".equals(subscriptionStatus);
    }
    
    /**
     * Actualiza el estado premium basado en la suscripción
     */
    public void updatePremiumStatus() {
        this.isUserPremium = isActiveSubscription();
    }


   public static UserSubscription fromDTO(UserSubscriptionUpdateDTO dto) {
        UserSubscription subscription = new UserSubscription();
        subscription.setAppUserId(dto.getAppUserId());
        subscription.setCurrentProductId(dto.getCurrentProductId());
        subscription.setExpirationAtMs(dto.getExpirationAtMs());
        subscription.setStore(dto.getStore());
        subscription.setOriginalTransactionId(dto.getOriginalTransactionId());
        subscription.setEnvironment(dto.getEnvironment());
        subscription.setCountryCode(dto.getCountryCode());
        subscription.setCurrency(dto.getCurrency());
        subscription.setLastEventTimestampMs(dto.getLastEventTimestampMs());
        subscription.setIsTestAccount(dto.getIsTestAccount());
        subscription.setSubscriptionStatus(dto.getSubscriptionStatus());
        subscription.setIsUserPremium(dto.getIsUserPremium());
        subscription.setUpdatedAt(dto.getUpdatedAt());
        return subscription;
    }
}