package com.hotty.chat_service.usecases.messages;

import org.springframework.stereotype.Service;

import com.hotty.chat_service.model.MessageModel;
import com.hotty.chat_service.repo.MessageModelRepo;

import reactor.core.publisher.Flux;

@Service
public class GetMessagesByUserUIDUseCase {

    private final MessageModelRepo messageModelRepo;

    public GetMessagesByUserUIDUseCase(MessageModelRepo messageModelRepo) {
        this.messageModelRepo = messageModelRepo;
    }

    /**
     * Retrieves messages for a given user UID.
     *
     * @param userUID The unique identifier of the user whose messages are to be
     *                retrieved.
     * @return A Flux containing the list of messages for the specified user UID.
     *         Returns empty Flux if no messages are found (which is a valid scenario).
     */
    public Flux<MessageModel> execute(String userUID) {
        if (userUID == null || userUID.isBlank()) {
            return Flux.error(new IllegalArgumentException("User UID cannot be null or blank."));
        }
        return messageModelRepo.findBySenderIdOrRecieverId(userUID);
    }

}
