package com.hotty.chat_service.usecases.messages;
import com.hotty.common.enums.PublishEventType;
import com.hotty.common.services.EventPublishers.ChatEventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.hotty.chat_service.repo.MessageModelRepo;

import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Use case responsible for handling the logic of marking a list of messages as read.
 * It interacts with the {@link MessageModelRepo} to update the message statuses.
 */
@Service
public class MarkMessagesAsReadUseCase {

    private final ChatEventPublisher publisher;

    private static final Logger log = LoggerFactory.getLogger(MarkMessagesAsReadUseCase.class);
    private final MessageModelRepo messageModelRepo;

    /**
     * Constructs a new MarkMessagesAsReadUseCase.
     *
     * @param messageModelRepo The repository for message data operations.
     * @throws IllegalArgumentException if messageModelRepo is null.
     */
    public MarkMessagesAsReadUseCase(MessageModelRepo messageModelRepo, ChatEventPublisher publisher) {

        this.messageModelRepo = messageModelRepo;
       
        this.publisher = publisher;
    }

    /**
     * Executes the process of marking specified messages as read.
     * The userUID parameter is currently used for logging and context,
     * as the repository method marks messages based on their UIDs directly.
     *
     * @param messageUIDs A list of unique identifiers (business messageId) of the messages to be marked as read.
     * @param userUID The unique identifier of the user performing this action (for logging/context).
     * @return A {@link Mono<Void>} that completes when the messages are successfully marked as read.
     *         If an error occurs, the Mono will emit a {@link MarkMessagesAsReadException}.
     * @throws IllegalArgumentException if messageUIDs list is null/empty or userUID is null/blank.
     */
    public Mono<Void> execute(List<String> messageUIDs, String userUID) { // Assuming messageUIDs are the business 'messageId'
        if (messageUIDs == null || messageUIDs.isEmpty()) {
            log.warn("Attempted to mark messages as read with an empty or null list of messageUIDs.");
            return Mono.error(new IllegalArgumentException("MessageUIDs cannot be empty."));
        }
        if (userUID == null || userUID.trim().isEmpty()) {
            log.warn("Attempted to mark messages (UIDs: {}) as read with a blank userUID.", messageUIDs);
            return Mono.error(new IllegalArgumentException("UserUID cannot be blank."));
        }

        return messageModelRepo.markMessagesAsReadByIds(messageUIDs)
                .flatMap(updatedMessage -> {
                    log.debug("Publicando actualización de estado 'leído' para el mensaje: {}", updatedMessage.getMessageId());
                    // Publicar la actualización a ambos participantes del chat.
                    Mono<Void> publishToSender = publisher.publishMessageUpdated(updatedMessage, updatedMessage.getSenderId());
                    Mono<Void> publishToReceiver = publisher.publishMessageUpdated(updatedMessage, updatedMessage.getRecieverId());

                    // Esperar a que ambas publicaciones se completen antes de continuar con el siguiente mensaje.
                    return Mono.zip(publishToSender, publishToReceiver);
                })
                .then() // Convierte el Flux de resultados en un Mono<Void> que se completa cuando todo ha terminado.
                .doOnError(e -> log.error("Error durante el proceso de marcar mensajes como leídos para el usuario {}: {}", userUID, e.getMessage(), e))
                .onErrorMap(e -> {
                    if (e instanceof MarkMessagesAsReadException || e instanceof IllegalArgumentException) {
                        return e;
                    }
                    return new MarkMessagesAsReadException(
                        String.format("Falló al marcar los mensajes como leídos para el usuario '%s'.", userUID), e);
                });
    }

    /**
     * Custom exception to indicate an issue during the process of marking messages as read
     * within this use case.
     */
    public static class MarkMessagesAsReadException extends RuntimeException {
        public MarkMessagesAsReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
