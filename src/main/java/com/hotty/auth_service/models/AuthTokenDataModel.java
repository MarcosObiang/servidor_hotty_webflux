package com.hotty.auth_service.models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Document(collection = "auth_tokens")
@Data
public class AuthTokenDataModel {

  
    @NotBlank(message = "User UID is required")
    private String userUID;
    @Indexed(unique = true)
    @NotBlank(message = "Token UID is required")
    @Id
    private String tokenUID;
    @NotBlank(message = "Token is required")
    @Indexed(unique = true)
    private String token;
    private boolean isRevoked = false;
    private boolean isExpired = false;

    private boolean isRefreshToken;

    private Instant issuedAt;

    @Indexed(expireAfter ="0s")
    private Instant expiresAt;

}
