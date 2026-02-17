package com.hotty.subscriptions_service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hotty.user_service.DTOs.UserSubscriptionUpdateDTO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class UserSubscriptionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserSubscriptionNotificationService.class);

    // Inyectar los beans del sink y stream
    private final Sinks.Many<UserSubscriptionUpdateDTO> subscriptionUpdatesSink;
    private final Flux<UserSubscriptionUpdateDTO> subscriptionUpdatesStream;

    public UserSubscriptionNotificationService(
            Sinks.Many<UserSubscriptionUpdateDTO> subscriptionUpdatesSink,
            Flux<UserSubscriptionUpdateDTO> subscriptionUpdatesStream) {
        this.subscriptionUpdatesSink = subscriptionUpdatesSink;
        this.subscriptionUpdatesStream = subscriptionUpdatesStream;
        log.info("✅ UserSubscriptionNotificationService initialized with shared stream beans");
    }

    /**
     * Envía una actualización de suscripción al stream compartido
     */
    public Mono<Void> sendSubscriptionUpdate(UserSubscriptionUpdateDTO updateDTO) {
        log.info("📤 Sending subscription update to shared stream for user: {}", updateDTO.getUserUID());

        return Mono.<Void>fromRunnable(() -> {
            Sinks.EmitResult result = subscriptionUpdatesSink.tryEmitNext(updateDTO);
            
            if (result.isSuccess()) {
                log.info("✅ Subscription update sent successfully to shared stream for user: {}", 
                    updateDTO.getUserUID());
            } else {
                log.warn("⚠️ Failed to emit subscription update to shared stream for user {}: {}", 
                    updateDTO.getUserUID(), result);
                // Reintentar con FAIL_FAST
                subscriptionUpdatesSink.emitNext(updateDTO, Sinks.EmitFailureHandler.FAIL_FAST);
            }
        })
        .doOnError(error -> log.error("❌ Error sending subscription update for user {}: {}", 
            updateDTO.getUserUID(), error.getMessage()));
    }

    /**
     * Obtiene el stream compartido de actualizaciones de suscripción
     */
    public Flux<UserSubscriptionUpdateDTO> getSubscriptionUpdatesStream() {
        log.debug("📡 Providing shared subscription updates stream");
        return subscriptionUpdatesStream;
    }

    /**
     * Stream filtrado para un usuario específico
     */
    public Flux<UserSubscriptionUpdateDTO> getSubscriptionUpdatesForUser(String userId) {
        log.debug("🔍 Providing filtered subscription updates stream for user: {}", userId);
        return subscriptionUpdatesStream
                .filter(update -> userId.equals(update.getUserUID()))
                .doOnNext(update -> log.debug("✨ Filtered subscription update for user {}: {}", 
                    userId, update.getEventType()));
    }

    /**
     * Cierra el sink compartido
     */
    public void close() {
        log.info("🔒 Closing shared subscription updates stream");
        subscriptionUpdatesSink.tryEmitComplete();
    }
}
