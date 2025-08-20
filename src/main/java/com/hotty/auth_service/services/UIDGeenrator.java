package com.hotty.auth_service.services;

import java.util.UUID;

public class UIDGeenrator {

    public static String generateUID(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero");
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");

        if (length >= uuid.length()) {
            return uuid;
        } else {
            return uuid.substring(0, length);
        }
    }

    public static String generateUID(){
        return UUID.randomUUID().toString().replace("-", "");
    }

}
