package com.hotty.common.services.PushNotifications.Strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.hotty.common.enums.NotificationDataType;
import com.hotty.common.enums.NotificationProvider;
import com.hotty.common.interfaces.NotificationStrategy;
import com.hotty.common.services.PushNotifications.Localization.LocalizatedMessages;
import com.hotty.user_service.model.UserNotificationDataModel;
import reactor.core.publisher.Mono;

/**
 * Estrategia de notificaciones push usando Firebase Cloud Messaging (FCM)
 * Ahora envía mensajes completamente localizados en lugar de usar loc-keys
 */
@Component
public class FCMStrategy implements NotificationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FCMStrategy.class);
    
    private final LocalizatedMessages localizedMessages;

    public FCMStrategy(LocalizatedMessages localizedMessages) {
        this.localizedMessages = localizedMessages;
    }

    @Override
    public Mono<Void> sendNotification(NotificationDataType type, UserNotificationDataModel notificationDataModel) {
        final Message msg;
        String deviceToken = notificationDataModel.getNotificationToken();
        
        if (deviceToken == null || deviceToken.isEmpty()) {
            logger.warn("⚠️ Device token is null or empty, skipping notification");
            return Mono.just(Void.TYPE).then();
        }

        // Obtener mensajes localizados para el idioma del usuario
        String localizedTitle = localizedMessages.getTitle(type, notificationDataModel.getLocale());
        String localizedMessage = localizedMessages.getMessage(type, notificationDataModel.getLocale());
        
        logger.debug("📱 Sending FCM notification - Type: {}, Locale: {}, Provider: {}", 
                type, notificationDataModel.getLocale(), notificationDataModel.getProvider());

        if (notificationDataModel.getProvider() == NotificationProvider.ANDROID) {
            // Obtener el canal según el tipo de notificación
            String channelId = getChannelIdForType(type);
            
            AndroidNotification androidNotification = AndroidNotification.builder()
                    .setTitle(localizedTitle)
                    .setBody(localizedMessage)
                    .setChannelId(channelId)  // ✅ Canal específico para cada tipo
                    .build();
            
            msg = Message.builder()
                    .setToken(deviceToken)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(androidNotification)
                            .build())
                    .build();
                    
            logger.debug("📱 Android notification prepared with channel: {}", channelId);
        }
        else if (notificationDataModel.getProvider() == NotificationProvider.IOS) {
            // iOS ahora también usa mensajes localizados directamente
            Notification iosNotification = Notification.builder()
                    .setTitle(localizedTitle)
                    .setBody(localizedMessage)
                    .build();
            
            msg = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(iosNotification)
                    .build();
        } else {
            msg = null;
        }

        return Mono.fromRunnable(() -> {
            try {
                if (msg == null) {
                    logger.warn("⚠️ Message is null, unsupported notification provider: {}", 
                            notificationDataModel.getProvider());
                    return;
                }
                
                String messageId = FirebaseMessaging.getInstance().send(msg);
                logger.info("✅ FCM notification sent successfully - MessageId: {}, Type: {}, Locale: {}", 
                        messageId, type, notificationDataModel.getLocale());
                        
            } catch (Exception e) {
                logger.error("❌ Error sending FCM notification - Type: {}, Provider: {}, Error: {}", 
                        type, notificationDataModel.getProvider(), e.getMessage(), e);
                throw new RuntimeException("Error sending FCM notification", e);
            }
        });
    }

    /**
     * Determina el ID del canal de notificación según el tipo
     * Estos IDs deben coincidir con los canales definidos en Flutter
     */
    private String getChannelIdForType(NotificationDataType type) {
        switch (type) {
            case CHAT:
                return "chat_notifications";
            case LIKE:
                return "social_notifications"; 
            case MESSAGE:
                return "message_notifications";
            default:
                return "general_notifications";
        }
    }

}
