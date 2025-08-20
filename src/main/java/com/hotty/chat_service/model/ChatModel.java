package com.hotty.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chats")
public class ChatModel {

    @Id
    private String chatId;

    private Instant chatCreationTimestamp;

    private String user1Id;
    private String user2Id;

    private String user1Name;
    private String user2Name;

    private String user1Picture;
    private String user2Picture;

    private boolean user1Blocked;
    private boolean user2Blocked;

    private String user1NotificationToken;
    private String user2NotificationToken;
}
