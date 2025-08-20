package com.hotty.user_service.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hotty.common.advice.GlobalExceptionHandler;
import com.hotty.ApiResponse.ApiResponse;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        ResponseEntity<ApiResponse<Void>> response = responseMono.block();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getStatus());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testHandleIllegalArgumentException() {
        String errorMessage = "Invalid user data";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getStatus());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testHandleRuntimeException() {
        String errorMessage = "Database connection failed";
        RuntimeException exception = new RuntimeException(errorMessage);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleRuntimeException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getStatus());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testHandleGenericException() {
        String errorMessage = "Unexpected error";
        Exception exception = new Exception(errorMessage);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("An unexpected error occurred"));
        assertTrue(response.getBody().getMessage().contains(errorMessage));
        assertNull(response.getBody().getData());
    }

    @Test
    void testHandleNullPointerException() {
        NullPointerException exception = new NullPointerException("Null value encountered");

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleNullPointerException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getStatus());
        assertEquals("Null value encountered", response.getBody().getMessage());
    }

    @Test
    void testHandleNullPointerExceptionWithNullMessage() {
        NullPointerException exception = new NullPointerException();

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleNullPointerException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getStatus());
        assertEquals("Unexpected null value", response.getBody().getMessage());
    }

    @Test
    void testHandleRuntimeExceptionWithNullMessage() {
        RuntimeException exception = new RuntimeException((String) null);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleRuntimeException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getStatus());
        assertEquals("Internal server error", response.getBody().getMessage());
    }
}