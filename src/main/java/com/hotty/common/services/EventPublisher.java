package com.hotty.common.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.common.dto.EventWrapper;
import com.hotty.common.enums.PublishEventType;

import reactor.core.publisher.Mono;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publisher común para eventos en todo el monolito.
 * Permite a todos los servicios publicar eventos de manera uniforme.
 */
@Service
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    // Canal único para todos los eventos
    private static final String EVENTS_CHANNEL = "monolith:events";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica un evento de creación de recurso
     */
    public <T> Mono<Void> publishCreated(T resource, String resourceUID, String receiverUID, String dataType) {
        EventWrapper<T> event = new EventWrapper<>(PublishEventType.CREATE, resource, resourceUID, receiverUID, dataType);
        return publish(event);
    }

    /**
     * Publica un evento de actualización de recurso
     */
    public <T> Mono<Void> publishUpdated(T resource, String resourceUID, String receiverUID, String dataType) {
        EventWrapper<T> event = new EventWrapper<>(PublishEventType.UPDATE, resource, resourceUID, receiverUID, dataType);
        return publish(event);
    }

    /**
     * Publica un evento de eliminación de recurso
     */
    public <T> Mono<Void> publishDeleted(T resource, String resourceUID, String receiverUID, String dataType) {
        EventWrapper<T> event = new EventWrapper<>(PublishEventType.DELETE, resource, resourceUID, receiverUID, dataType);
        return publish(event);
    }

    /**
     * Publica un evento genérico
     */
    public <T> Mono<Void> publishEvent(PublishEventType eventType, T resource, String resourceUID, String receiverUID, String dataType) {
        EventWrapper<T> event = new EventWrapper<>(eventType, resource, resourceUID, receiverUID, dataType);
        return publish(event);
    }

    /**
     * Publica un evento directamente
     */
    public <T> Mono<Void> publish(EventWrapper<T> event) {
        return Mono.fromCallable(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                log.debug("Publishing event to channel {}: {}", EVENTS_CHANNEL, eventJson);
                return eventJson;
            } catch (Exception e) {
                log.error("Error serializing event: {}", e.getMessage());
                throw new RuntimeException("Failed to serialize event", e);
            }
        })
        .flatMap(eventJson -> 
            reactiveRedisTemplate.convertAndSend(EVENTS_CHANNEL, eventJson)
                .doOnSuccess(result -> log.debug("Event published successfully. Subscribers notified: {}", result))
                .doOnError(error -> log.error("Error publishing event: {}", error.getMessage()))
                .onErrorMap(error -> new RuntimeException("Failed to publish event", error))
        )
        .then();
    }

    /**
     * Publica un evento a un canal específico
     */
    public <T> Mono<Void> publishToChannel(String channel, EventWrapper<T> event) {
        return Mono.fromCallable(() -> {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                log.debug("Publishing event to channel {}: {}", channel, eventJson);
                return eventJson;
            } catch (Exception e) {
                log.error("Error serializing event: {}", e.getMessage());
                throw new RuntimeException("Failed to serialize event", e);
            }
        })
        .flatMap(eventJson -> 
            reactiveRedisTemplate.convertAndSend(channel, eventJson)
                .doOnSuccess(result -> log.debug("Event published successfully to channel {}. Subscribers notified: {}", channel, result))
                .doOnError(error -> log.error("Error publishing event to channel {}: {}", channel, error.getMessage()))
                .onErrorMap(error -> new RuntimeException("Failed to publish event to channel: " + channel, error))
        )
        .then();
    }
}
