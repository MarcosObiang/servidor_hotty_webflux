package com.hotty.chat_service.usecases.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.hotty.chat_service.model.MessageModel;
import com.hotty.chat_service.repo.MessageModelRepo;

import reactor.core.publisher.Flux;

/**
 * Use case responsible for retrieving messages associated with a specific chat.
 * It interacts with the {@link MessageModelRepo} to fetch the message data.
 */
@Service
public class GetMessagesUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetMessagesUseCase.class);
    private final MessageModelRepo messageRepository;

    /**
     * Constructs a new GetMessagesUseCase.
     *
     * @param messageRepository The repository for message data operations.
     * @throws IllegalArgumentException if messageRepository is null.
     */
    public GetMessagesUseCase(MessageModelRepo messageRepository) {
        if (messageRepository == null) {
            throw new IllegalArgumentException("MessageModelRepo cannot be null.");
        }
        this.messageRepository = messageRepository;
    }

    /**
     * Retrieves all messages associated with a specific chat UID.
     *
     * @param chatUID The unique identifier of the chat whose messages are to be retrieved.
     * @return A {@link Flux<MessageModel>} emitting the messages found for the given chat UID.
     *         Returns an empty Flux if no messages are found.
     *         If a repository error occurs, the Flux will emit a {@link GetMessagesException}.
     * @throws IllegalArgumentException if chatUID is null or blank.
     */
    public Flux<MessageModel> execute(String chatUID) {
        if (chatUID == null || chatUID.trim().isEmpty()) {
            log.warn("Attempted to get messages with a null or blank chatUID.");
            return Flux.error(new IllegalArgumentException("ChatUID cannot be blank."));
        }

        return messageRepository.findByChatUID(chatUID)
                .doOnNext(message -> log.debug("Retrieved message ID: {} for chatUID: {}", message.getMessageId(), chatUID))
                .doOnComplete(() -> log.info("Successfully retrieved messages for chatUID: {}. Stream completed.", chatUID))
                .doOnError(error ->
                        // The repository already logs DataAccessExceptions in detail.
                        // This log provides context at the use case level for any failure.
                        log.error("Failed to retrieve messages for chatUID='{}': {}",
                                chatUID, error.getMessage(), error))
                .onErrorMap(e -> {
                    // Avoid re-wrapping exceptions already handled or specific to this use case.
                    if (e instanceof GetMessagesException || e instanceof IllegalArgumentException) {
                        return e;
                    }
                    // Wrap other errors (like DataAccessException from repo) in a use-case-specific exception.
                    return new GetMessagesException(
                        String.format("Error retrieving messages for chatUID '%s'.", chatUID), e);
                });
                // Note: switchIfEmpty is removed. Returning an empty Flux is often preferred
                // for "get" operations when no data is found, rather than an error.
    }

    /**
     * Custom exception to indicate an issue during the message retrieval process
     * within this use case.
     */
    public static class GetMessagesException extends RuntimeException {
        public GetMessagesException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
