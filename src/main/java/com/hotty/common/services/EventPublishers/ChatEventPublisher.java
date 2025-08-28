package com.hotty.common.services.EventPublishers;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.chat_service.model.ChatModel;
import com.hotty.chat_service.model.MessageModel;
import com.hotty.common.dto.EventWrapper;
import com.hotty.common.enums.PublishEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

@Component
public class ChatEventPublisher {


     private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    // Canal único para todos los eventos de usuario. Los suscriptores escucharán
    // aquí.
    private static final String USER_EVENTS_CHANNEL = "user:events";
    private static final String MESSAGE_dATA_TYPE = "message";
    private static final String CHAT_DATA_TYPE = "chat";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public ChatEventPublisher(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> publishMessageCreated(MessageModel user, String userUID) {
        EventWrapper<MessageModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.CREATE);
        event.setBody(user);
        event.setResourceUID(user.getMessageId());
        event.setReceiverUID(userUID); // El usuario creado es el receptor del evento.
        event.setDataType(MESSAGE_dATA_TYPE);
        return publish(event);
    }

    public Mono<Void> publishMessageUpdated(MessageModel user, String userUID) {
        EventWrapper<MessageModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.UPDATE);
        event.setBody(user);
        event.setResourceUID(user.getMessageId());
        event.setReceiverUID(userUID); // El usuario actualizado es el receptor del evento.
        event.setDataType(MESSAGE_dATA_TYPE);
        return publish(event);
    }

    public Mono<Void> publishMessageDeleted(MessageModel deletedLike, String userUID) {
        EventWrapper<MessageModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.DELETED);
        event.setBody(deletedLike); // Enviar el objeto completo para consistencia.
        event.setResourceUID(deletedLike.getMessageId());
        event.setDataType(MESSAGE_dATA_TYPE);
        // Notificar al receptor original que el like fue eliminado.
        event.setReceiverUID(userUID);
        return publish(event);
    }

    public Mono<Void> publishChatCreated(ChatModel user, String userUID) {
        EventWrapper<ChatModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.CREATE);
        event.setBody(user);
        event.setResourceUID(user.getChatId());
        event.setReceiverUID(userUID); // El usuario creado es el receptor del evento.
        event.setDataType(CHAT_DATA_TYPE);
        return publish(event);
    }

    public Mono<Void> publishChatUpdated(ChatModel user, String userUID) {
        EventWrapper<ChatModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.UPDATE);
        event.setBody(user);
        event.setResourceUID(user.getChatId());
        event.setReceiverUID(userUID); // El usuario actualizado es el receptor del evento.
        event.setDataType(CHAT_DATA_TYPE);
        return publish(event);
    }

    public Mono<Void> publishChatDeleted(ChatModel user, String userUID) {
        EventWrapper<ChatModel> event = new EventWrapper<>();
        event.setEventType(PublishEventType.DELETED);
        event.setBody(user); // Enviar el objeto completo para consistencia.
        event.setResourceUID(user.getChatId());
        event.setDataType(CHAT_DATA_TYPE);
        // Notificar al receptor original que el like fue eliminado.
        event.setReceiverUID(userUID);
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
