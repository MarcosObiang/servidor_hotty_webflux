package com.hotty.auth_service.Exceptions;

public class NotFoundExceptionCustom extends RuntimeException {
    public NotFoundExceptionCustom(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}


