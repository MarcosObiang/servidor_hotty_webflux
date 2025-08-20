package com.hotty.chat_service.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank; // More suitable for String fields than @NotNull
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Data // Generates getters, setters, equals, hashCode, and toString
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
@Document(collection = "messages")
public class MessageModel {

    @Id
    private String id;

    @NotBlank(message = "chatUID cannot be blank") // Checks for null and empty/whitespace strings
    @Field("chatUID") // Changed from conversationId
    private String chatUID;

    @NotNull(message = "createdAt cannot be null")
    @Field("created_at") // Changed from timestamp
    private Instant createdAt; // Changed from Long to Instant

    @NotNull(message = "readByReciever cannot be null")
    @Field("readByReciever")
    private Boolean readByReciever; // Assuming this field remains, if not, it should be removed or updated.

    @NotBlank(message = "senderId cannot be blank")
    @Field("senderId")
    private String senderId;

    @NotBlank(message = "recieverId cannot be blank")
    @Field("recieverId")
    private String recieverId;

    @NotBlank(message = "messageContent cannot be blank")
    @Field("messageContent")
    private String messageContent;

    @NotBlank(message = "messageType cannot be blank")
    @Field("messageType")
    private String messageType;

    @NotBlank(message = "messageId cannot be blank")
    @Field("messageId")
    private String messageId;
}