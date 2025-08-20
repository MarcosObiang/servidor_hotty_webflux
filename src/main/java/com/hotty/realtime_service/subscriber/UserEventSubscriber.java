
package com.hotty.realtime_service.subscriber;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotty.common.dto.EventWrapper;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
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

                    // Emitimos el evento directamente al sink, que lo enviará a los clientes WebSocket.
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
}