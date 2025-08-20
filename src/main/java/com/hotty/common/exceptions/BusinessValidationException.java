package com.hotty.common.exceptions;

/**
 * Excepción para errores de validación de negocio.
 * Usada cuando las reglas de negocio no se cumplen.
 */
public class BusinessValidationException extends RuntimeException {
    
    private final String errorCode;

    public BusinessValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessValidationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
