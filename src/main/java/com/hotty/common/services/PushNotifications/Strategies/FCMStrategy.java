package com.hotty.common.services.PushNotifications.Strategies;

import org.springframework.stereotype.Component;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.hotty.common.enums.NotificationDataType;
import com.hotty.common.interfaces.NotificationStrategy;
import com.hotty.common.interfaces.NotificationStrategy;
import reactor.core.publisher.Mono;

@Component
public class FCMStrategy implements NotificationStrategy {

    @Override
    public Mono<Void> sendNotification(String deviceToken, NotificationDataType type) {
        Message msg = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle("hotty")
                        .setBody(type.toString())
                        .build())
                .build();

        return Mono.fromRunnable(() -> {
            try {
                FirebaseMessaging.getInstance().send(msg);
            } catch (Exception e) {
                throw new RuntimeException("Error sending FCM notification", e);
            }
        });
    }

}
