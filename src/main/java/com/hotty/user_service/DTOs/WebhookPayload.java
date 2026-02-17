package com.hotty.user_service.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookPayload {
    
    @JsonProperty("api_version")
    private String apiVersion;
    
    @JsonProperty("event")
    private WebhookEvent event;
    
}