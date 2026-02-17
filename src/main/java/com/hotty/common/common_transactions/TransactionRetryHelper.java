package com.hotty.common.common_transactions;

import com.mongodb.MongoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Function;

@Component
public class TransactionRetryHelper {

    private static final Logger log = LoggerFactory.getLogger(TransactionRetryHelper.class);
    
    // Configuración de reintentos
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(100);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(2);
    
    /**
     * Ejecuta una operación con reintentos automáticos para errores transitorios
     * @param operation La operación a ejecutar
     * @param operationName Nombre de la operación para logging
     * @return Mono con el resultado
     */
    public <T> Mono<T> executeWithRetry(Mono<T> operation, String operationName) {
        return operation
                .doOnSubscribe(subscription -> 
                    log.debug("Executing operation: {}", operationName))
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .maxBackoff(MAX_BACKOFF)
                        .filter(this::isRetryableException)
                        .doBeforeRetry(retrySignal -> 
                            log.warn("Retrying operation '{}' due to transient error. Attempt: {}/{}, Error: {}", 
                                    operationName, 
                                    retrySignal.totalRetries() + 1, 
                                    MAX_RETRIES,
                                    retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Operation '{}' failed after {} retries. Last error: {}", 
                                    operationName, MAX_RETRIES, retrySignal.failure().getMessage());
                            return new TransactionRetryExhaustedException(
                                    "Operation '" + operationName + "' failed after " + MAX_RETRIES + " retries", 
                                    retrySignal.failure());
                        }))
                .doOnSuccess(result -> 
                    log.debug("Operation '{}' completed successfully", operationName))
                .doOnError(error -> 
                    log.error("Operation '{}' failed with non-retryable error: {}", operationName, error.getMessage()));
    }
    
    /**
     * Versión con función personalizada para mayor flexibilidad
     */
    public <T> Mono<T> executeWithRetry(Function<Void, Mono<T>> operationFunction, String operationName) {
        return executeWithRetry(operationFunction.apply(null), operationName);
    }
    
    /**
     * Determina si una excepción es reintentable
     */
    private boolean isRetryableException(Throwable throwable) {
        // Errores específicos de MongoDB que son transitorios
        if (throwable instanceof MongoException) {
            MongoException mongoException = (MongoException) throwable;
            int errorCode = mongoException.getCode();
            
            // Códigos de error transitorios comunes en MongoDB
            switch (errorCode) {
                case 251: // NoSuchTransaction - El error que estás experimentando
                case 244: // TransactionNumberTooOld  
                case 246: // UnknownTransactionCommitResult
                case 11601: // InterruptedAtShutdown
                case 11602: // Interrupted
                case 50: // ExceededTimeLimit
                case 89: // NetworkTimeout
                case 7: // HostNotFound
                case 6: // HostUnreachable
                case 112: // WriteConflict
                case 117: // ConflictingOperationInProgress
                    log.debug("Detected retryable MongoDB error code {}: {}", errorCode, throwable.getMessage());
                    return true;
                default:
                    log.debug("Non-retryable MongoDB error code {}: {}", errorCode, throwable.getMessage());
                    return false;
            }
        }
        
        // Errores de red, timeout y transacciones que pueden ser transitorios
        String errorMessage = throwable.getMessage();
        if (errorMessage != null) {
            String lowerCaseMessage = errorMessage.toLowerCase();
            
            // Errores transitorios específicos de transacciones
            if (lowerCaseMessage.contains("no such transaction") ||
                lowerCaseMessage.contains("transaction number") ||
                lowerCaseMessage.contains("transient transaction") ||
                lowerCaseMessage.contains("transaction coordinator") ||
                lowerCaseMessage.contains("session not found")) {
                log.debug("Detected transaction-related transient error: {}", throwable.getMessage());
                return true;
            }
            
            // Errores de red y timeout
            boolean isNetworkError = lowerCaseMessage.contains("connection") ||
                                   lowerCaseMessage.contains("timeout") ||
                                   lowerCaseMessage.contains("network") ||
                                   lowerCaseMessage.contains("socket");
            
            if (isNetworkError) {
                log.debug("Detected network-related error: {}", throwable.getMessage());
                return true;
            }
        }
        
        log.debug("Non-retryable error: {}", throwable.getMessage());
        return false;
    }
    
    /**
     * Excepción personalizada cuando se agotan los reintentos
     */
    public static class TransactionRetryExhaustedException extends RuntimeException {
        public TransactionRetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
