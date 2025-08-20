package com.hotty.realtime_service.model;
import java.time.Instant;

public class LikeData {
    private String senderUID;
    private String petName;
    private Instant createdAt;
    private String petPictureURL;
    private String senderName;
    private String likeUID;
    private String likedPetUID;
    private String receiverUID;
    private boolean isRevealed;
    private String senderPictureURL;

    // getters y setters

    public String getSenderUID() {
        return senderUID;
    }
    public void setSenderUID(String senderUID) {
        this.senderUID = senderUID;
    }
    public String getPetName() {
        return petName;
    }
    public void setPetName(String petName) {
        this.petName = petName;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public String getPetPictureURL() {
        return petPictureURL;
    }
    public void setPetPictureURL(String petPictureURL) {
        this.petPictureURL = petPictureURL;
    }
    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    public String getLikeUID() {
        return likeUID;
    }
    public void setLikeUID(String likeUID) {
        this.likeUID = likeUID;
    }
    public String getLikedPetUID() {
        return likedPetUID;
    }
    public void setLikedPetUID(String likedPetUID) {
        this.likedPetUID = likedPetUID;
    }
    public String getReceiverUID() {
        return receiverUID;
    }
    public void setReceiverUID(String receiverUID) {
        this.receiverUID = receiverUID;
    }
}
