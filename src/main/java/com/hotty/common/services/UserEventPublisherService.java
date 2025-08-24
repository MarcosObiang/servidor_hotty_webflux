package com.hotty.common.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.common.dto.EventWrapper;
import com.hotty.common.enums.PublishEventType;
import com.hotty.user_service.model.UserDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserEventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisherService.class);

    // Canal único para todos los eventos de usuario. Los suscriptores escucharán
    // aquí.
    private static final String USER_EVENTS_CHANNEL = "user:events";
    private static final String USER_DATA_TYPE = "user";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public UserEventPublisherService(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> publishUserCreated(UserDataModel user) {
        EventWrapper<UserDataModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.CREATE);
        event.setBody(user);
        event.setResourceUID(user.getUserUID());
        event.setReceiverUID(user.getUserUID()); // El usuario creado es el receptor del evento.
        event.setDataType(USER_DATA_TYPE);
        return publish(event);
    }

    public Mono<Void> publishUserUpdated(UserDataModel user) {
        EventWrapper<UserDataModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.UPDATE);
        event.setBody(user);
        event.setResourceUID(user.getUserUID());
        event.setReceiverUID(user.getUserUID()); // El usuario actualizado es el receptor del evento.
        event.setDataType(USER_DATA_TYPE);
        return publish(event);
    }

    public Mono<Void> publishUserDeleted(String userUID) {
        EventWrapper<String> event = new EventWrapper<>();
        event.setEventType(PublishEventType.DELETED);
        event.setBody(userUID); // Para la eliminación, el cuerpo puede ser simplemente el UID.
        event.setResourceUID(userUID);
        event.setDataType(USER_DATA_TYPE);
        event.setReceiverUID(userUID); // El usuario eliminado es el receptor del evento.
        return publish(event);
    }

    private <T> Mono<Void> publish(EventWrapper<T> event) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(eventJson -> reactiveRedisTemplate.convertAndSend(USER_EVENTS_CHANNEL, eventJson))
                .doOnSuccess(clients -> log.info(
                        "Evento de usuario '{}' para recurso '{}' publicado en '{}'. Notificado a {} suscriptores.",
                        event.getEventType(), event.getResourceUID(), USER_EVENTS_CHANNEL, clients))
                .doOnError(e -> log.error("Error al serializar o publicar evento de usuario: {}", event, e))
                .then();
    }
}