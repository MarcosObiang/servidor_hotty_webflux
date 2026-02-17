package com.hotty.common.services.PushNotifications.Localization;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.hotty.user_service.enums.LocalizationCodes;

import lombok.Data;

/**
 * Configuración para el servicio de localización de notificaciones
 * Permite personalizar el comportamiento del sistema de localización
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.notifications")
public class LocalizationConfig {
    
    /**
     * Idioma de fallback cuando no se encuentra la traducción solicitada
     */
    private LocalizationCodes fallbackLocale = LocalizationCodes.EN;
    
    /**
     * Habilitar logging detallado para debugging de localización
     */
    private boolean debugEnabled = false;
    
    /**
     * Habilitar carga lazy de mensajes (futuro feature)
     */
    private boolean lazyLoading = false;
    
}
