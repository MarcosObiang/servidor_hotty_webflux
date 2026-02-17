package com.hotty.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotty.user_service.DTOs.UserSubscriptionUpdateDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Configuración del stream global de notificaciones de suscripción
 * Permite que múltiples servicios emitan y escuchen actualizaciones de suscripción
 */
@Configuration
public class SubscriptionNotificationStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionNotificationStreamConfig.class);

    /**
     * Bean del Sink para actualizaciones de suscripción
     * Permite que múltiples servicios emitan eventos al mismo stream
     */
    @Bean
    public Sinks.Many<UserSubscriptionUpdateDTO> subscriptionUpdatesSink() {
        log.info("Creating subscription updates sink bean");
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    /**
     * Bean del Flux compartido para actualizaciones de suscripción
     * Permite que múltiples servicios se suscriban al mismo stream
     */
    @Bean
    public Flux<UserSubscriptionUpdateDTO> subscriptionUpdatesStream(
            Sinks.Many<UserSubscriptionUpdateDTO> subscriptionUpdatesSink) {

        log.info("Creating subscription updates stream bean");
        return subscriptionUpdatesSink.asFlux()
                .doOnSubscribe(subscription -> log.info("🔗 New subscriber to global subscription updates stream"))
                .doOnCancel(() -> log.info("❌ Subscriber cancelled from global subscription updates stream"))
                .doOnNext(update -> log.debug("📡 Broadcasting subscription update for user: {}", update.getUserUID()))
                .share(); // Compartir entre múltiples suscriptores
    }

}
