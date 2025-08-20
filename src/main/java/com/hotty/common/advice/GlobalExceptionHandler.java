package com.hotty.common.advice;

import com.hotty.ApiResponse.ApiResponse;
import com.hotty.common.exceptions.BusinessValidationException;
import com.hotty.common.exceptions.DownstreamServiceException;
import com.hotty.common.exceptions.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para todo el monolito.
 * Captura excepciones de todos los módulos (user_service, auth_service, chat_service, etc.)
 * y las mapea a respuestas HTTP estandarizadas usando ApiResponse común.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de tipo ResourceNotFoundException.
     * Usada cuando no se encuentra un recurso específico.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage()))
        );
    }

    /**
     * Maneja excepciones de tipo BusinessValidationException.
     * Usada cuando las reglas de negocio no se cumplen.
     */
    @ExceptionHandler(BusinessValidationException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBusinessValidationException(BusinessValidationException ex) {
        logger.warn("Business validation failed: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()))
        );
    }

    /**
     * Maneja excepciones de tipo DownstreamServiceException.
     * Usada cuando falla la comunicación entre servicios internos.
     */
    @ExceptionHandler(DownstreamServiceException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleDownstreamServiceException(DownstreamServiceException ex) {
        logger.error("Downstream service error from {}: {}", ex.getServiceName(), ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode()) != null ? 
                           HttpStatus.resolve(ex.getStatusCode()) : HttpStatus.INTERNAL_SERVER_ERROR;
        return Mono.just(
                ResponseEntity.status(status)
                        .body(ApiResponse.error("DOWNSTREAM_SERVICE_ERROR", 
                              "Error communicating with " + ex.getServiceName() + ": " + ex.getMessage()))
        );
    }

    /**
     * Maneja excepciones de tipo NoSuchElementException.
     * Usado cuando no se encuentra un recurso (usuario, chat, like, etc.).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleNoSuchElementException(NoSuchElementException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("NOT_FOUND", ex.getMessage()))
        );
    }

    /**
     * Maneja excepciones de tipo IllegalArgumentException.
     * Usado para errores de validación de entrada (parámetros inválidos).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()))
        );
    }

    /**
     * Maneja excepciones de tipo IllegalStateException.
     * Usado para conflictos de estado (duplicados, operaciones no permitidas).
     */
    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalStateException(IllegalStateException ex) {
        logger.warn("Illegal state (conflict): {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("CONFLICT", ex.getMessage()))
        );
    }

    /**
     * Maneja excepciones de validación de Bean Validation (ConstraintViolationException).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        
        logger.warn("Validation failed: {}", message);
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("VALIDATION_ERROR", message))
        );
    }

    /**
     * Maneja excepciones de validación de WebFlux (WebExchangeBindException).
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleWebExchangeBindException(WebExchangeBindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> "'" + error.getField() + "': " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        logger.warn("WebFlux validation failed: {}", message);
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("VALIDATION_ERROR", message))
        );
    }

    /**
     * Maneja excepciones de entrada de servidor web (ServerWebInputException).
     */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleServerWebInputException(ServerWebInputException ex) {
        logger.warn("Server web input error: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("BAD_REQUEST", "Invalid input: " + ex.getReason()))
        );
    }

    /**
     * Maneja RuntimeExceptions genéricas no capturadas por otros handlers.
     */
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleRuntimeException(RuntimeException ex) {
        logger.error("Unexpected runtime exception: {}", ex.getMessage(), ex);
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred"))
        );
    }

    /**
     * Maneja todas las demás excepciones no capturadas específicamente.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(Exception ex) {
        logger.error("Unexpected exception: {}", ex.getMessage(), ex);
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred"))
        );
    }
}
