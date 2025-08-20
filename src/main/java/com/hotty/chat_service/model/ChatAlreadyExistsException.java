package com.hotty.chat_service.model;

public class ChatAlreadyExistsException extends RuntimeException {

    public ChatAlreadyExistsException(String message) {
        super(message);
    }
}