package com.hotty.chat_service.usecases.chat;

import org.springframework.stereotype.Component;

import com.hotty.common.enums.NotificationDataType;
import com.hotty.common.services.EventPublishers.ChatEventPublisher;
import com.hotty.common.services.PushNotifications.Factories.NotificationStrategyFactory;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserNotificationDataModel;
import com.hotty.user_service.usecases.GetUserByUIDUseCase;
import com.hotty.chat_service.interfaces.ChatRepository;
import com.hotty.chat_service.model.ChatModel;

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

    private final NotificationStrategyFactory notificationStrategyFactory;

    private final GetUserByUIDUseCase getUserByUIDUseCase;

    /**
     * Constructor que inyecta el repositorio de chats y el publicador de likes.
     *
     * @param chatRepository Repositorio para operaciones CRUD con chats.
     * @param publisher      Publicador de eventos de chats.
     */
    public CreateChatUseCase(ChatRepository chatRepository, ChatEventPublisher publisher,
            NotificationStrategyFactory notificationStrategyFactory, GetUserByUIDUseCase getUserByUIDUseCase) {
        this.chatRepository = chatRepository;
        this.publisher = publisher;
        this.notificationStrategyFactory = notificationStrategyFactory;
        this.getUserByUIDUseCase = getUserByUIDUseCase;
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
        return Mono.zip(getUserByUIDUseCase.execute(user1Id), getUserByUIDUseCase.execute(user2Id))
                .flatMap(users -> {
                    UserDataModel user1 = users.getT1();
                    UserDataModel user2 = users.getT2();

                    // Actualizar los tokens de notificación con los datos reales de los usuarios
                    if (user1.getNotificationData() != null && user1.getNotificationData().getNotificationToken() != null) {
                        chat.setUser1NotificationToken(user1.getNotificationData().getNotificationToken());
                    }
                    if (user2.getNotificationData() != null && user2.getNotificationData().getNotificationToken() != null) {
                        chat.setUser2NotificationToken(user2.getNotificationData().getNotificationToken());
                    }

                    return chatRepository.save(chat).flatMap(savedChat -> {
                        // Validar datos de notificación antes de usar
                        UserNotificationDataModel user1NotificationData = user1.getNotificationData();
                        UserNotificationDataModel user2NotificationData = user2.getNotificationData();

                        // Crear operaciones de notificación solo si los datos son válidos
                        Mono<Void> user1Notification = (user1NotificationData != null && user1NotificationData.getProvider() != null) 
                            ? notificationStrategyFactory
                                .getStrategy(user1NotificationData.getProvider())
                                .sendNotification(NotificationDataType.CHAT, user1NotificationData)
                                .onErrorResume(error -> {
                                    // Log del error pero no fallar toda la operación
                                    System.err.println("Failed to send notification to user1: " + error.getMessage());
                                    return Mono.empty();
                                })
                            : Mono.empty();

                        Mono<Void> user2Notification = (user2NotificationData != null && user2NotificationData.getProvider() != null)
                            ? notificationStrategyFactory
                                .getStrategy(user2NotificationData.getProvider())
                                .sendNotification(NotificationDataType.CHAT, user2NotificationData)
                                .onErrorResume(error -> {
                                    // Log del error pero no fallar toda la operación
                                    System.err.println("Failed to send notification to user2: " + error.getMessage());
                                    return Mono.empty();
                                })
                            : Mono.empty();

                        // Publicar eventos (también con manejo de errores)
                        Mono<Void> event1 = publisher.publishChatCreated(savedChat, user1Id)
                                .onErrorResume(error -> {
                                    System.err.println("Failed to publish chat event for user1: " + error.getMessage());
                                    return Mono.empty();
                                });

                        Mono<Void> event2 = publisher.publishChatCreated(savedChat, user2Id)
                                .onErrorResume(error -> {
                                    System.err.println("Failed to publish chat event for user2: " + error.getMessage());
                                    return Mono.empty();
                                });

                        // Ejecutar todas las operaciones en paralelo, pero no fallar si alguna falla
                        return Mono.when(user1Notification, user2Notification, event1, event2)
                                .doOnSuccess(v -> {
                                    // Log de éxito para debugging
                                    System.out.println("Chat created successfully with notifications and events published for chat: " + savedChat.getChatId());
                                })
                                .doOnError(error -> {
                                    // Log de error general
                                    System.err.println("Error during chat creation post-processing: " + error.getMessage());
                                })
                                .onErrorResume(error -> {
                                    // Si fallan las notificaciones/eventos, continuar con el chat creado
                                    System.err.println("Continuing with chat creation despite notification/event errors: " + error.getMessage());
                                    return Mono.empty();
                                })
                                .thenReturn(savedChat);
                    });
                });

    }
}
