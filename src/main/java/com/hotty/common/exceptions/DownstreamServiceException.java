package com.hotty.common.exceptions;

/**
 * Excepción personalizada para errores de servicios downstream.
 * Usada cuando falla la comunicación entre servicios internos del monolito.
 */
public class DownstreamServiceException extends RuntimeException {
    
    private final String serviceName;
    private final int statusCode;

    public DownstreamServiceException(String serviceName, int statusCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public DownstreamServiceException(String serviceName, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
