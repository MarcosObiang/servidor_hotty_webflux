package com.hotty.chat_service.usecases.chat;

import org.springframework.stereotype.Component;

import com.hotty.common.dto.EventWrapper;
import com.hotty.common.enums.PublishEventType;
import com.hotty.chat_service.interfaces.ChatRepository;
import com.hotty.chat_service.model.ChatModel;
import com.hotty.common.services.ChatEventPublisher;

import reactor.core.publisher.Mono;

import java.time.Instant;

import java.util.UUID;

/**
 * Caso de uso para crear un chat entre dos usuarios.
 * <p>
 * Este caso de uso crea directamente un nuevo chat con la información básica de
 * ambos usuarios.
 * </p>
 */
@Component
public class CreateChatUseCase {

    private final ChatRepository chatRepository;

    private final ChatEventPublisher publisher;

    /**
     * Constructor que inyecta el repositorio de chats y el publicador de likes.
     *
     * @param chatRepository Repositorio para operaciones CRUD con chats.
     * @param publisher      Publicador de eventos de chats.
     */
    public CreateChatUseCase(ChatRepository chatRepository, ChatEventPublisher publisher) {
        this.chatRepository = chatRepository;
        this.publisher = publisher;
    }

    /**
     * Ejecuta la lógica para crear un chat entre dos usuarios.
     * <p>
     * - Valida que los IDs de usuario no sean iguales.
     * - Crea un nuevo chat con la información proporcionada.
     * </p>
     *
     * @param user1Id         ID del primer usuario.
     * @param user1Name       Nombre del primer usuario.
     * @param user1PictureURL URL de la imagen del primer usuario.
     * @param user2Id         ID del segundo usuario.
     * @param user2Name       Nombre del segundo usuario.
     * @param user2PictureURL URL de la imagen del segundo usuario.
     * @return Mono que emite el chat creado o existente.
     * @throws IllegalArgumentException si los IDs de usuario son iguales.
     */
    public Mono<ChatModel> execute(String user1Id, String user1Name, String user1PictureURL,
            String user2Id, String user2Name, String user2PictureURL) {

        ChatModel chat = new ChatModel();
        chat.setChatId(UUID.randomUUID().toString());
        chat.setUser1Id(user1Id);
        chat.setUser1Name(user1Name);
        chat.setUser1Picture(user1PictureURL);
        chat.setUser1Blocked(false);
        chat.setUser1NotificationToken(""); // Considerar si este token se debe obtener de algún lado

        chat.setUser2Id(user2Id);
        chat.setUser2Name(user2Name);
        chat.setUser2Picture(user2PictureURL);
        chat.setUser2Blocked(false);
        chat.setUser2NotificationToken(""); // Considerar si este token se debe obtener de algún lado

        chat.setChatCreationTimestamp(Instant.now());
        

        // Guardar el nuevo chat en la base de datos
        return chatRepository.save(chat).flatMap(savedChat -> {
            // Publicar el evento de creación del chat

            return Mono
                    .zip(publisher.publishChatCreated(savedChat, user1Id),
                            publisher.publishChatCreated(savedChat, user2Id))
                    .thenReturn(savedChat);
        });

    }
}
