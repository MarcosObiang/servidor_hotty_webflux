package com.hotty.chat_service.usecases.chat;

import org.springframework.stereotype.Service;

import com.hotty.chat_service.interfaces.ChatRepository;
import com.hotty.chat_service.model.ChatModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class GetChatsByUserUIDUseCase {

    private final ChatRepository chatRepository;

    public GetChatsByUserUIDUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public Flux<ChatModel> execute(String userUID) {
        return chatRepository.findByUserId(userUID);
    }

}
