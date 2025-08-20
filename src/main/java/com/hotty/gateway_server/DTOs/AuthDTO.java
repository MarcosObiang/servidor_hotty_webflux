package com.hotty.gateway_server.DTOs;

public class AuthDTO {
    private String token;
    private String userUID;
    private String expiresAt;
    
    // Constructor, getters y setters

    public AuthDTO(String token, String userUID, String expiresAt) {
        this.token = token;
        this.userUID = userUID;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
