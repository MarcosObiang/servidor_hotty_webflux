package com.hotty.subscriptions_service.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RevenueCatWebhookPayload {
    
    @JsonProperty("api_version")
    private String apiVersion;
    
    @JsonProperty("event")
    private RevenueCatWebhookEvent event;
    
}