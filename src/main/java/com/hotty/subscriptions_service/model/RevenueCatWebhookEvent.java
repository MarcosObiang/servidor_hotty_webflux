package com.hotty.subscriptions_service.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Document(collection = "revenueCatWebhooks")
public class RevenueCatWebhookEvent {

    @Id
    private String id; // Será el event.id del webhook
    
    @JsonProperty("api_version")
    private String apiVersion;
    
    // Datos del evento principal
    private EventData event;
    
    @Data
    public static class EventData {
        private List<String> aliases;
        
        @JsonProperty("app_id")
        private String appId;
        
        @JsonProperty("app_user_id")
        private String appUserId;
        
        @JsonProperty("commission_percentage")
        private Double commissionPercentage;
        
        @JsonProperty("country_code")
        private String countryCode;
        
        private String currency;
        
        @JsonProperty("entitlement_id")
        private String entitlementId;
        
        @JsonProperty("entitlement_ids")
        private List<String> entitlementIds;
        
        private String environment; // PRODUCTION, SANDBOX
        
        @JsonProperty("event_timestamp_ms")
        private Long eventTimestampMs;
        
        @JsonProperty("expiration_at_ms")
        private Long expirationAtMs;
        
        private String id;
        
        @JsonProperty("is_family_share")
        private Boolean isFamilyShare;
        
        @JsonProperty("offer_code")
        private String offerCode;
        
        @JsonProperty("original_app_user_id")
        private String originalAppUserId;
        
        @JsonProperty("original_transaction_id")
        private String originalTransactionId;
        
        @JsonProperty("period_type")
        private String periodType; // NORMAL, TRIAL, INTRO
        
        @JsonProperty("presented_offering_id")
        private String presentedOfferingId;
        
        private Double price;
        
        @JsonProperty("price_in_purchased_currency")
        private Double priceInPurchasedCurrency;
        
        @JsonProperty("product_id")
        private String productId;
        
        @JsonProperty("new_product_id")
        private String newProductId; // Para eventos PRODUCT_CHANGE - el nuevo producto
        
        @JsonProperty("purchased_at_ms")
        private Long purchasedAtMs;
        
        private String store; // APP_STORE, PLAY_STORE, STRIPE, etc.
        
        @JsonProperty("subscriber_attributes")
        private Map<String, SubscriberAttribute> subscriberAttributes;
        
        @JsonProperty("takehome_percentage")
        private Double takehomePercentage;
        
        @JsonProperty("tax_percentage")
        private Double taxPercentage;
        
        @JsonProperty("transaction_id")
        private String transactionId;
        
        private String type; // INITIAL_PURCHASE, RENEWAL, CANCELLATION, etc.
    }
    
    @Data
    public static class SubscriberAttribute {
        @JsonProperty("updated_at_ms")
        private Long updatedAtMs;
        private String value;
    }
    
    // Helper methods para acceder a datos del evento
    public String getAppUserId() {
        return event != null ? event.getAppUserId() : null;
    }
    
    public String getEffectiveAppUserId() {
        String appUserId = getAppUserId();
        return (appUserId != null && !appUserId.trim().isEmpty()) ? appUserId : null;
    }
    
    public String getProductId() {
        return event != null ? event.getProductId() : null;
    }
    
    public String getNewProductId() {
        return event != null ? event.getNewProductId() : null;
    }
    
    // Para eventos PRODUCT_CHANGE, obtiene el producto anterior
    public String getPreviousProductId() {
        return getProductId(); // En PRODUCT_CHANGE, product_id es el producto anterior
    }
    
    // Para eventos PRODUCT_CHANGE, obtiene el producto nuevo
    public String getCurrentProductId() {
        String newProductId = getNewProductId();
        return newProductId != null ? newProductId : getProductId(); // Fallback al product_id
    }
}
