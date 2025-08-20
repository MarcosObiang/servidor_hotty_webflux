package com.hotty.user_service.exceptions;

import com.hotty.common.advice.GlobalExceptionHandler;
import com.hotty.common.exceptions.BusinessValidationException;
import com.hotty.common.exceptions.ResourceNotFoundException;
import com.hotty.ApiResponse.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleNoSuchElementException() {
        String errorMessage = "No se encontró un usuario con el UID: test-uid";
        NoSuchElementException exception = new NoSuchElementException(errorMessage);

        Mono<ResponseEntity<ApiResponse<Void>>> responseMono = globalExceptionHandler.handleNoSuchElementException(exception);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("NOT_FOUND", response.getBody().getStatus());
                    assertEquals(errorMessage, response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }

    @Test
    void testHandleIllegalArgumentException() {
        String errorMessage = "Argumento inválido";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        Mono<ResponseEntity<ApiResponse<Void>>> responseMono = globalExceptionHandler.handleIllegalArgumentException(exception);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("BAD_REQUEST", response.getBody().getStatus());
                    assertEquals(errorMessage, response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "123");

        Mono<ResponseEntity<ApiResponse<Void>>> responseMono = globalExceptionHandler.handleResourceNotFoundException(exception);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("RESOURCE_NOT_FOUND", response.getBody().getStatus());
                    assertEquals("User with id '123' not found", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }

    @Test
    void testHandleBusinessValidationException() {
        BusinessValidationException exception = new BusinessValidationException("INSUFFICIENT_CREDITS", "No tienes suficientes créditos");

        Mono<ResponseEntity<ApiResponse<Void>>> responseMono = globalExceptionHandler.handleBusinessValidationException(exception);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("INSUFFICIENT_CREDITS", response.getBody().getStatus());
                    assertEquals("No tienes suficientes créditos", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }

    @Test
    void testHandleRuntimeException() {
        RuntimeException exception = new RuntimeException("Error inesperado");

        Mono<ResponseEntity<ApiResponse<Void>>> responseMono = globalExceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getStatus());
                    assertEquals("An unexpected error occurred", response.getBody().getMessage());
                    assertNull(response.getBody().getData());
                })
                .verifyComplete();
    }
}
