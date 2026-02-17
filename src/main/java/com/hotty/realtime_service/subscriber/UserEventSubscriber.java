
package com.hotty.realtime_service.subscriber;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotty.common.dto.EventWrapper;
import com.hotty.realtime_service.WebSocketSessionManager;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Mono;

@Component
public class UserEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(UserEventSubscriber.class);
    private static final String USER_EVENTS_CHANNEL = "user:events";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final Sinks.Many<EventWrapper<Map<String, Object>>> sink;
    private final ObjectMapper objectMapper;

    public UserEventSubscriber(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                               Sinks.Many<EventWrapper<Map<String, Object>>> sink) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.sink = sink;
        // Configuramos un ObjectMapper para que pueda manejar tipos de Java 8 como Instant
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void subscribeToUserEvents() {
        this.reactiveRedisTemplate
            .listenToChannel(USER_EVENTS_CHANNEL)
            .doOnSubscribe(subscription -> log.info("Suscrito al canal de Redis: '{}'", USER_EVENTS_CHANNEL))
            .flatMap(message -> {
                try {
                    // Deserializamos el evento. El cuerpo (body) será un Map.
                    EventWrapper<Map<String, Object>> event = objectMapper.readValue(message.getMessage(), new TypeReference<>() {});
                    log.info("Evento de usuario recibido: Tipo='{}', DataType='{}', Recurso='{}'",
                            event.getEventType(), event.getDataType(), event.getResourceUID());

                    // 🔔 MANEJO ESPECIAL PARA REVOCACIÓN DE TOKENS
                    if ("token_revocation".equals(event.getDataType())) {
                        return handleTokenRevocationEvent(event);
                    }

                    // Para otros eventos, los emitimos normalmente al sink
                    sink.tryEmitNext(event);
                    return Mono.empty(); // Completamos el procesamiento para este mensaje.

                } catch (JsonProcessingException e) {
                    log.error("Error al deserializar el evento de usuario desde Redis: {}", message.getMessage(), e);
                    return Mono.empty(); // Ignoramos el mensaje si no se puede procesar.
                }
            })
            .doOnError(error -> log.error("Error en la suscripción de Redis al canal '{}'.", USER_EVENTS_CHANNEL, error))
            .subscribe(); // ¡Es crucial llamar a subscribe() para que el listener se active!
    }

    /**
     * Maneja eventos de revocación de tokens cerrando la sesión WebSocket del usuario afectado.
     * 
     * @param event El evento de revocación de token
     * @return Mono<Void> que se completa cuando se procesa el evento
     */
    private Mono<Void> handleTokenRevocationEvent(EventWrapper<Map<String, Object>> event) {
        String userUID = event.getReceiverUID();
        String tokenUID = event.getResourceUID();
        
        if (userUID == null || userUID.isBlank()) {
            log.warn("Evento de revocación de token sin userUID válido: {}", event);
            return Mono.empty();
        }

        // Buscar y cerrar la sesión WebSocket del usuario
        WebSocketSession session = WebSocketSessionManager.getSession(userUID);
        
        if (session != null && session.isOpen()) {
            log.info("Cerrando sesión WebSocket para usuario '{}' debido a revocación de token '{}'", userUID, tokenUID);
            
            // Obtener información adicional del evento para logging
            Map<String, Object> body = event.getBody();
            String revocationType = body != null ? (String) body.get("revocationType") : "UNKNOWN";
            String reason = body != null ? (String) body.get("reason") : "Token revoked";
            
            log.info("Tipo de revocación: '{}' - Razón: '{}'", revocationType, reason);
            
            // Cerrar la sesión con un código específico para revocación de token
            return session.close(new CloseStatus(4003, "Token revoked: " + reason))
                .doOnSuccess(v -> log.info("Sesión WebSocket cerrada exitosamente para usuario '{}' (tokenUID: '{}')", userUID, tokenUID))
                .doOnError(e -> log.error("Error al cerrar sesión WebSocket para usuario '{}' (tokenUID: '{}'): {}", userUID, tokenUID, e.getMessage()));
        } else {
            log.debug("No hay sesión WebSocket activa para usuario '{}' (tokenUID: '{}')", userUID, tokenUID);
            return Mono.empty();
        }
    }
}