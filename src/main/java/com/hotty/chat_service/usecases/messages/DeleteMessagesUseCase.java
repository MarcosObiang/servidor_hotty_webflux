package com.hotty.chat_service.usecases.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.hotty.chat_service.repo.MessageModelRepo;

import reactor.core.publisher.Mono;

/**
 * Use case responsible for handling the logic of deleting all messages associated with a specific chat.
 * It interacts with the {@link MessageModelRepo} to perform the deletion.
 */
@Service
public class DeleteMessagesUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteMessagesUseCase.class);
    private final MessageModelRepo messageModelRepo;

    /**
     * Constructs a new DeleteMessagesUseCase.
     *
     * @param messageModelRepo The repository for message data operations.
     */
    public DeleteMessagesUseCase(MessageModelRepo messageModelRepo) {
        if (messageModelRepo == null) {
            throw new IllegalArgumentException("MessageModelRepo cannot be null.");
        }
        this.messageModelRepo = messageModelRepo;
    }

    /**
     * Deletes all messages associated with a specific chat.
     *
     * @param chatUID The unique identifier (UID) of the chat whose messages are to be deleted.
     * @return A {@link Mono<Void>} that completes when the deletion is successful.
     *         If an error occurs, the Mono will emit a {@link DeleteMessagesException}.
     * @throws IllegalArgumentException if chatUID is null or blank.
     */
    public Mono<Void> execute(String chatUID) {
        if (chatUID == null || chatUID.trim().isEmpty()) {
            log.warn("Attempted to delete messages with a null or blank chatUID.");
            return Mono.error(new IllegalArgumentException("ChatUID cannot be blank."));
        }

        return messageModelRepo.deleteByChatUID(chatUID)
                .doOnSuccess(deletedCount ->
                        log.info("{} messages deleted successfully for chatUID: {}", deletedCount, chatUID))
                .doOnError(error ->
                        // The repository already logs DataAccessExceptions in detail.
                        // This log provides context at the use case level for any failure.
                        log.error("Failed to delete messages for chatUID='{}': {}",
                                chatUID, error.getMessage(), error))
                .onErrorMap(e -> {
                    // Avoid re-wrapping exceptions already handled or specific to this use case.
                    if (e instanceof DeleteMessagesException || e instanceof IllegalArgumentException) {
                        return e;
                    }
                    // Wrap other errors (like DataAccessException from repo) in a use-case-specific exception.
                    return new DeleteMessagesException(
                        String.format("Error deleting messages for chatUID '%s'.", chatUID), e);
                })
                .then(); // Signals completion on success, or propagates the (mapped) error.
    }

    /**
     * Custom exception to indicate an issue during the message deletion process
     * within this use case.
     */
    public static class DeleteMessagesException extends RuntimeException {
        public DeleteMessagesException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
