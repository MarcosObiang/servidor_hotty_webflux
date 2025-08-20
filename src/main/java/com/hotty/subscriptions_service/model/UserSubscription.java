package com.hotty.subscriptions_service.model;

import lombok.Data;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "userSubscriptions")
public class UserSubscription {

    @Id
    private String id; // Usa el app_user_id de RevenueCat como _id en MongoDB
    private String userId; // Nuevo: ID del usuario en la app (opcional, si se necesita)
    private Boolean isUserPremium;
    private String subscriptionStatus; // Ej: SUBSCRIBED, CANCELLATION, PAYMENT_ISSUES, etc.
    private String subscriptionId; // El product_id de RevenueCat
    private String entitlement; // Nuevo: El derecho de acceso, como "hotty_plus"
    private Long expirationAtMs; // Renombrado: El timestamp de caducidad en milisegundos
    private List<String> entitlementIds; // Nuevo: Lista de IDs de derechos de acceso
    private String expiresDate; // Nuevo: La fecha de caducidad en formato ISO 8601
    private String store; // El proveedor de la tienda (APP_STORE, PLAY_STORE, etc.)
    private String originalTransactionId; // Nuevo: El ID de la transacción original
    private Boolean isTestAccount; // Nuevo: Para diferenciar las transacciones de prueba
    private Long subscriptionPausedAtMs; // Nuevo: Timestamp de inicio de la pausa
    private Long endSubscriptionPauseAtMs; // Nuevo: Timestamp de fin de la pausa
    private Long lastSeenAtMs; // Nuevo: Timestamp del último evento de webhook
}