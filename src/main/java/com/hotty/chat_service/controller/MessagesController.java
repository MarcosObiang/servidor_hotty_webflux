package com.hotty.chat_service.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hotty.ApiResponse.ApiResponse;
import com.hotty.chat_service.model.MessageModel;
import com.hotty.chat_service.usecases.messages.DeleteMessagesUseCase;
import com.hotty.chat_service.usecases.messages.GetMessagesByUserUIDUseCase;
import com.hotty.chat_service.usecases.messages.GetMessagesUseCase;
import com.hotty.chat_service.usecases.messages.MarkMessagesAsReadUseCase;
import com.hotty.chat_service.usecases.messages.SendMessageUseCase;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {

    private static final Logger log = LoggerFactory.getLogger(MessagesController.class);

    private final GetMessagesUseCase getMessagesUseCase;
    private final MarkMessagesAsReadUseCase markMessagesAsReadUseCase;
    private final DeleteMessagesUseCase deleteMessagesUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessagesByUserUIDUseCase getMessagesByUserUIDUseCase;

    public MessagesController(GetMessagesUseCase getMessagesUseCase,
            MarkMessagesAsReadUseCase markMessagesAsReadUseCase,
            GetMessagesByUserUIDUseCase getMessagesByUserUIDUseCase,
            DeleteMessagesUseCase deleteMessagesUseCase,
            SendMessageUseCase sendMessageUseCase) {
        this.getMessagesUseCase = getMessagesUseCase;
        this.markMessagesAsReadUseCase = markMessagesAsReadUseCase;
        this.deleteMessagesUseCase = deleteMessagesUseCase;
        this.sendMessageUseCase = sendMessageUseCase;
        this.getMessagesByUserUIDUseCase = getMessagesByUserUIDUseCase;
    }

    @GetMapping("/by-user")
    public Mono<ResponseEntity<ApiResponse<List<MessageModel>>>> getMessages(
            @RequestHeader("userUID") String userUID) {
        log.info("GET /api/messages - Request for userUID: {}", userUID);
        return getMessagesByUserUIDUseCase.execute(userUID)
                .collectList()
                .map(messages -> ResponseEntity.ok(ApiResponse.success("Messages retrieved for user " + userUID, messages)));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Void>>> sendMessage(
            @Valid @RequestBody MessageModel message,
            @RequestHeader("userUID") String userUID) {
        log.info("POST /api/messages - Request to send message for chatUID: {}, by userUID: {}", message.getChatUID(), userUID);
        return sendMessageUseCase.execute(message, userUID)
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message sent successfully.")));
    }

    @PutMapping("/read")
    public Mono<ResponseEntity<ApiResponse<Void>>> markMessagesAsRead(
            @RequestBody List<String> messageUIDs,
            @RequestHeader("userUID") String userUID) {
        log.info("PUT /api/messages/read - Request by userUID: {}. MessageUIDs count: {}", userUID, messageUIDs != null ? messageUIDs.size() : "null");
        return markMessagesAsReadUseCase.execute(messageUIDs, userUID)
                .thenReturn(ResponseEntity.ok(ApiResponse.success("Messages marked as read.")));
    }

    @DeleteMapping
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteMessages(
            @RequestParam String chatUID,
            @RequestHeader("userUID") String userUID) {
        log.info("DELETE /api/messages - Request for chatUID: {}, by userUID: {}", chatUID, userUID);
        return deleteMessagesUseCase.execute(chatUID)
                .thenReturn(ResponseEntity.ok(ApiResponse.success("Messages for chat " + chatUID + " deleted.")));
    }

    @GetMapping("/by-chat/{chatUID}")
    public Mono<ResponseEntity<ApiResponse<List<MessageModel>>>> getMessagesByChatUID(
            @PathVariable String chatUID,
            @RequestHeader("userUID") String userUID) {
        log.info("GET /api/messages/by-chat/{} - Request for chatUID: {}, userUID: {}", chatUID, chatUID, userUID);
        return getMessagesUseCase.execute(chatUID)
                .collectList()
                .map(messages -> ResponseEntity.ok(ApiResponse.success("Messages retrieved for chat " + chatUID, messages)));
    }
}
