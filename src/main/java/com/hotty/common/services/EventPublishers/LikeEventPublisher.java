package com.hotty.common.services.EventPublishers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.common.dto.EventWrapper;
import com.hotty.common.enums.PublishEventType;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.user_service.model.UserDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LikeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LikeEventPublisher.class);

    

    // Canal único para todos los eventos de usuario. Los suscriptores escucharán
    // aquí.
    private static final String USER_EVENTS_CHANNEL = "user:events";
    private static final String USER_DATA_TYPE = "like";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public LikeEventPublisher(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }


    /**
     * Publica un evento de creación de Like.
     * en el canal de eventos de usuario.
     * ubicado en redis para que este disponible para el escuchador donde el usuario este conectado
     * @param user
     * @return
     */

    public Mono<Void> publishLikeCreated(LikeModel user) {
        EventWrapper<LikeModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.CREATE);
        event.setBody(user);
        event.setResourceUID(user.getLikeUID());
        event.setReceiverUID(user.getReceiverUID()); // El usuario creado es el receptor del evento.
        event.setDataType(USER_DATA_TYPE);
        return publish(event);
    }


    /**
     * Publica un evento de actualización de Like.
     * en el canal de eventos de usuario.
     * ubicado en redis para que este disponible para el escuchador donde el usuario este conectado
     * @param user
     * @return
     */

    public Mono<Void> publishLikeUpdated(LikeModel user) {
        EventWrapper<LikeModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.UPDATE);
        event.setBody(user);
        event.setResourceUID(user.getLikeUID());
        event.setReceiverUID(user.getReceiverUID()); // El usuario actualizado es el receptor del evento.
        event.setDataType(USER_DATA_TYPE);
        return publish(event);
    }

    /**
     * Publica un evento de eliminación de Like.
     * en el canal de eventos de usuario.
     * ubicado en redis para que este disponible para el escuchador donde el usuario este conectado
     * @param deletedLike
     * @return
     */
    public Mono<Void> publishLikeDeleted(LikeModel deletedLike) {
        EventWrapper<LikeModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.DELETED);
        event.setBody(deletedLike); // Enviar el objeto completo para consistencia.
        event.setResourceUID(deletedLike.getLikeUID());
        event.setDataType(USER_DATA_TYPE);
        // Notificar al receptor original que el like fue eliminado.
        event.setReceiverUID(deletedLike.getReceiverUID());
        return publish(event);
    }


    /**
     * Método genérico para publicar eventos en Redis.
     *
     * @param event El evento a publicar.
     * @param <T>   Tipo de dato del cuerpo del evento.
     * @return Mono<Void> que indica la finalización de la publicación.
     */

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