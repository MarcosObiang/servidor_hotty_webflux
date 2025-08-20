package com.hotty.likes_service.model;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents a 'Like' action from one user to another in the system.
 * This document is stored in the 'likes' collection in MongoDB.
 */
@Document(collection = "likes")
@JsonInclude(JsonInclude.Include.ALWAYS)
@Data
@NoArgsConstructor
public class LikeModel {

    @Id
    private String id;

    @NonNull
    @Indexed(unique = true)
    private String likeUID;

    @NonNull
    @Indexed
    private String senderUID;

    @NonNull
    @Indexed
    private String receiverUID;

    private String senderPictureURL;
    private Boolean isRevealed = false;
    private LocalDate senderBirthDate;
    private String senderName;

    // This field is automatically populated by Spring Data MongoDB Auditing.
    // Ensure @EnableMongoAuditing is configured in your application.
    @CreatedDate
    private Instant createdAt;
    private Boolean isBlocked = false;
    private Integer likeValue;
    private Instant offerExpirationDate;
    @Version
    private Long version;

}
