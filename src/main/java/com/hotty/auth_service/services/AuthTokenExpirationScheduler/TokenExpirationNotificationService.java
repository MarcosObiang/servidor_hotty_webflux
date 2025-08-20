package com.hotty.auth_service.services.AuthTokenExpirationScheduler;

import org.springframework.stereotype.Service;

@Service
public class TokenExpirationNotificationService {

    public TokenExpirationNotificationService() {
    }

    public void notifyListeners(String userUID) {
       // kafkaTemplate.send("token-expiration-notification", userUID);
    }

}