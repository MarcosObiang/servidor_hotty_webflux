package com.hotty.chat_service.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hotty.ApiResponse.ApiResponse;
import com.hotty.chat_service.model.ChatModel;
import com.hotty.chat_service.usecases.chat.CreateChatUseCase;
import com.hotty.chat_service.usecases.chat.DeleteChatUseCase;
import com.hotty.chat_service.usecases.chat.DeleteChatsByUserUIDUseCase;
import com.hotty.chat_service.usecases.chat.GetChatsByUserUIDUseCase;

import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Controlador REST para operaciones relacionadas con chats.
 */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final CreateChatUseCase createChatUseCase;
    private final GetChatsByUserUIDUseCase getChatsByUserUIDUseCase;
    private final DeleteChatsByUserUIDUseCase deleteChatsByUserUIDUseCase;
    private final DeleteChatUseCase deleteChatUseCase;

    /**
     * Constructor para inyección de dependencias.
     */
    public ChatController(CreateChatUseCase createChatUseCase,
            GetChatsByUserUIDUseCase getChatsByUserUIDUseCase,
            DeleteChatsByUserUIDUseCase deleteChatsByUserUIDUseCase,
            DeleteChatUseCase deleteChatUseCase) {
        this.createChatUseCase = createChatUseCase;
        this.getChatsByUserUIDUseCase = getChatsByUserUIDUseCase;
        this.deleteChatsByUserUIDUseCase = deleteChatsByUserUIDUseCase;
        this.deleteChatUseCase = deleteChatUseCase;
    }

    /**
     * Crea un nuevo chat entre dos usuarios.
     */
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ChatModel>>> createChat(
            @RequestParam String user1Id,
            @RequestParam String user1Name,
            @RequestParam String user1PictureURL,
            @RequestParam String user2Id,
            @RequestParam String user2Name,
            @RequestParam String user2PictureURL) {

        return createChatUseCase.execute(user1Id, user1Name, user1PictureURL, user2Id, user2Name, user2PictureURL)
                .map(chat -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Chat created successfully.", chat)));
    }

    /**
     * Obtiene todos los chats de un usuario específico.
     */
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ChatModel>>>> getChatsByUserUID(@RequestHeader("userUID") String userUID) {
        return getChatsByUserUIDUseCase.execute(userUID)
                .collectList()
                .map(chats -> ResponseEntity.ok(ApiResponse.success("Chats retrieved for user " + userUID, chats)));
    }

    /**
     * Elimina todos los chats de un usuario específico.
     */
    @DeleteMapping("/delete-all")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteChatsByUserUID(@RequestHeader("userUID") String userUID) {
        return deleteChatsByUserUIDUseCase.execute(userUID)
                .thenReturn(ResponseEntity.ok(ApiResponse.success("All chats for user " + userUID + " deleted.")));
    }

    /**
     * Elimina un chat específico.
     */
    @DeleteMapping("/{chatId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteChat(@PathVariable String chatId) {
        return deleteChatUseCase.execute(chatId)
                .thenReturn(ResponseEntity.ok(ApiResponse.success("Chat " + chatId + " deleted successfully.")));
    }
}
