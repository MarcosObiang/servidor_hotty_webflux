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
    @Id
    private String id;

    @NotBlank(message = "User UID is required")
    @Indexed
    private String userUID;
    
    @Indexed(unique = true)
    @NotBlank(message = "Token UID is required")
    private String tokenUID;
    
    // ✅ ACCESS TOKEN
    @NotBlank(message = "Access token is required")
    @Indexed(unique = true)
    private String token;
    
    private boolean isRevoked = false;
    private boolean isExpired = false;
    private Instant issuedAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
    
    // ✅ REFRESH TOKEN - NUEVO CAMPO
    @NotBlank(message = "Refresh token is required")
    @Indexed(unique = true, sparse = true)
    private String refreshToken;
    
    // ✅ REFRESH TOKEN METADATA
    private boolean refreshTokenRevoked = false;
    private boolean refreshTokenExpired = false;
    private Instant refreshTokenExpiresAt;
    
    // ✅ HELPER METHODS
    public boolean isAccessTokenValid() {
        return !isRevoked && !isExpired && expiresAt != null && expiresAt.isAfter(Instant.now());
    }
    
    public boolean isRefreshTokenValid() {
        return !refreshTokenRevoked && !refreshTokenExpired && 
               refreshTokenExpiresAt != null && refreshTokenExpiresAt.isAfter(Instant.now());
    }
    
    public boolean isSessionValid() {
        return isAccessTokenValid() || isRefreshTokenValid();
    }
    
    public void revokeSession() {
        this.isRevoked = true;
        this.refreshTokenRevoked = true;
    }
}
