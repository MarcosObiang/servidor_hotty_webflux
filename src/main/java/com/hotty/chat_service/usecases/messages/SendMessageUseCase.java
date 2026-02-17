package com.hotty.chat_service.usecases.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.hotty.common.dto.EventWrapper;
import com.hotty.chat_service.model.MessageModel;
import com.hotty.chat_service.repo.MessageModelRepo;
import com.hotty.common.enums.NotificationDataType;
import com.hotty.common.enums.PublishEventType;
import com.hotty.common.services.EventPublishers.ChatEventPublisher;
import com.hotty.common.services.PushNotifications.Factories.NotificationStrategyFactory;
import com.hotty.user_service.model.UserNotificationDataModel;
import com.hotty.user_service.usecases.GetUserByUIDUseCase;

import ch.qos.logback.core.spi.ConfigurationEvent.EventType;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case responsible for handling the logic of sending (saving) a message.
 * It interacts with the {@link MessageModelRepo} to persist the message data.
 */
@Service
public class SendMessageUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendMessageUseCase.class);
    private final MessageModelRepo messageModelRepo;
    private final ChatEventPublisher publisher;
    private final GetUserByUIDUseCase getUserByUIDUseCase;
    private final NotificationStrategyFactory notificationStrategyFactory;

    /**
     * Constructs a new SendMessageUseCase.
     *
     * @param messageModelRepo The repository for message data operations.
     */
    public SendMessageUseCase(MessageModelRepo messageModelRepo, ChatEventPublisher publisher,
            GetUserByUIDUseCase getUserByUIDUseCase, NotificationStrategyFactory notificationStrategyFactory) {
        this.messageModelRepo = messageModelRepo;
        this.publisher = publisher;
        this.getUserByUIDUseCase = getUserByUIDUseCase;
        this.notificationStrategyFactory = notificationStrategyFactory;
    }

    /**
     * Executes the message sending process.
     * This involves setting the sender's UID, ensuring a creation timestamp,
     * and then attempting to save the message via the repository.
     *
     * @param message The {@link MessageModel} to be sent. It's expected to be
     *                populated
     *                with necessary details like chatUID, content, etc.
     * @param userUID The unique identifier of the user sending the message.
     * @return A {@link Mono<Void>} that completes upon successful saving of the
     *         message.
     *         If an error occurs during the process (e.g., database error), the
     *         Mono
     *         will emit a {@link SendMessageException}.
     * @throws IllegalArgumentException if message or userUID is null/blank.
     */
    public Mono<Void> execute(MessageModel message, String userUID) {
        if (message == null) {
            log.warn("Attempted to send a null message.");
            return Mono.error(new IllegalArgumentException("Message cannot be null."));
        }
        if (userUID == null || userUID.trim().isEmpty()) {
            // Log with message details if available, otherwise it might be too early
            log.warn("Attempted to send a message (chatUID: {}) with a blank userUID.", message.getChatUID());
            return Mono.error(new IllegalArgumentException("UserUID cannot be blank."));
        }

        message.setSenderId(userUID);
        message.setMessageId(UUID.randomUUID().toString());
        // Ensure creation timestamp is set if not already present
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Instant.now());
        }

        return getUserByUIDUseCase.execute(message.getRecieverId())
                .flatMap(user -> messageModelRepo.save(message)
                        .flatMap(savedMessage -> {
                            log.info(
                                    "Message id='{}' for chatUID='{}' by sender='{}' processed and saved successfully.",
                                    savedMessage.getId(), savedMessage.getChatUID(), userUID);

                            UserNotificationDataModel notificationDataModel = user.getNotificationData();

                            Mono<Void> sendPushNotificationMono = notificationStrategyFactory
                                    .getStrategy(notificationDataModel.getProvider())
                                    .sendNotification(NotificationDataType.MESSAGE, notificationDataModel);

                            // Use the saved message to publish, ensuring data consistency.
                            Mono<Void> publishToSender = publisher.publishMessageCreated(savedMessage,
                                    savedMessage.getSenderId());
                            Mono<Void> publishToReceiver = publisher.publishMessageCreated(savedMessage,
                                    savedMessage.getRecieverId());

                            // Execute both publications in parallel and complete when both are done.
                            return Mono.zip(publishToSender, publishToReceiver, sendPushNotificationMono).then();
                        })
                        .doOnError(error ->
                        // The repository already logs DataAccessExceptions in detail.
                        // This log provides context at the use case level for any failure.
                        log.error("Failed to process and save message for chatUID='{}', senderUID='{}': {}",
                                message.getChatUID(), userUID, error.getMessage(), error))
                        .onErrorMap(e -> {
                            // Avoid re-wrapping exceptions already handled or specific to this use case.
                            if (e instanceof SendMessageException || e instanceof IllegalArgumentException) {
                                return e;
                            }
                            // Wrap other errors (like DataAccessException from repo) in a use-case-specific
                            // exception.
                            return new SendMessageException(
                                    String.format("Error sending message for chatUID '%s' by sender '%s'.",
                                            message.getChatUID(), userUID),
                                    e);
                        }));
    }

    /**
     * Custom exception to indicate an issue during the message sending process
     * within this use case.
     */
    public static class SendMessageException extends RuntimeException {
        public SendMessageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
