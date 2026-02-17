package com.hotty.common.services.PushNotifications.Localization;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Contenedor inmutable para título y mensaje de notificación localizada
 */
@Data
@AllArgsConstructor
public class NotificationTexts {
    private final String title;
    private final String message;
}
