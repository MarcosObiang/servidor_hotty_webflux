package com.hotty.common.services.PushNotifications.Localization;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.hotty.common.enums.NotificationDataType;
import com.hotty.user_service.enums.LocalizationCodes;

import jakarta.annotation.PostConstruct;

/**
 * Servicio de localización para notificaciones push
 * Gestiona títulos y mensajes en múltiples idiomas con fallback automático
 */
@Component
public class LocalizatedMessages {

    private static final Logger logger = LoggerFactory.getLogger(LocalizatedMessages.class);

    @Value("${app.notifications.fallback-locale:EN}")
    private String fallbackLocaleName;

    private LocalizationCodes fallbackLocale;
    private final Map<NotificationDataType, Map<LocalizationCodes, NotificationTexts>> messages = new EnumMap<>(NotificationDataType.class);

    @PostConstruct
    public void init() {
        // Configurar fallback locale
        try {
            fallbackLocale = LocalizationCodes.valueOf(fallbackLocaleName);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid fallback locale '{}', using EN as default", fallbackLocaleName);
            fallbackLocale = LocalizationCodes.EN;
        }
        
        // Cargar mensajes desde archivos de propiedades
        loadMessagesFromFiles();
        
        logger.info("✅ LocalizedMessages initialized with {} notification types and fallback locale: {}", 
                messages.size(), fallbackLocale);
    }

    /**
     * Carga mensajes desde archivos de propiedades para todos los idiomas
     */
    private void loadMessagesFromFiles() {
        // Inicializar mapas para cada tipo de notificación
        Map<LocalizationCodes, NotificationTexts> chatTexts = new EnumMap<>(LocalizationCodes.class);
        Map<LocalizationCodes, NotificationTexts> likeTexts = new EnumMap<>(LocalizationCodes.class);
        Map<LocalizationCodes, NotificationTexts> messageTexts = new EnumMap<>(LocalizationCodes.class);
        
        // Cargar para cada idioma soportado
        for (LocalizationCodes locale : LocalizationCodes.values()) {
            Properties props = loadPropertiesForLocale(locale);
            if (props != null) {
                // Cargar notificaciones de chat
                String chatTitle = props.getProperty("chat.title", "New Chat");
                String chatMessage = props.getProperty("chat.message", "You have a new chat!");
                chatTexts.put(locale, new NotificationTexts(chatTitle, chatMessage));
                
                // Cargar notificaciones de likes
                String likeTitle = props.getProperty("like.title", "New Like");
                String likeMessage = props.getProperty("like.message", "You received a new like!");
                likeTexts.put(locale, new NotificationTexts(likeTitle, likeMessage));
                
                // Cargar notificaciones de mensajes
                String messageTitle = props.getProperty("message.title", "New Message");
                String messageMsg = props.getProperty("message.message", "You have a new message!");
                messageTexts.put(locale, new NotificationTexts(messageTitle, messageMsg));
                
                logger.debug("✅ Loaded messages for locale: {}", locale);
            } else {
                logger.warn("⚠️ Could not load properties for locale: {}", locale);
            }
        }
        
        // Asignar a los mapas principales
        messages.put(NotificationDataType.CHAT, chatTexts);
        messages.put(NotificationDataType.LIKE, likeTexts);
        messages.put(NotificationDataType.MESSAGE, messageTexts);
    }
    
    /**
     * Carga las propiedades para un idioma específico con fallback inteligente
     */
    private Properties loadPropertiesForLocale(LocalizationCodes locale) {
        // Intentar cargar el archivo específico primero
        Properties props = loadPropertiesFile(locale);
        
        // Si no encuentra archivo específico, intentar fallback genérico
        if (props == null && isRegionalLocale(locale)) {
            LocalizationCodes genericLocale = getGenericLocale(locale);
            logger.debug("Trying generic fallback {} for regional locale {}", genericLocale, locale);
            props = loadPropertiesFile(genericLocale);
        }
        
        return props;
    }
    
    /**
     * Carga un archivo de propiedades específico
     */
    private Properties loadPropertiesFile(LocalizationCodes locale) {
        String filename = String.format("messages/messages_%s.properties", locale.name().toLowerCase());
        Properties props = new Properties();
        
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            if (resource.exists()) {
                try (InputStreamReader reader = new InputStreamReader(
                        resource.getInputStream(), StandardCharsets.UTF_8)) {
                    props.load(reader);
                    logger.debug("✅ Loaded properties from: {}", filename);
                    return props;
                }
            } else {
                logger.debug("Properties file not found: {}", filename);
            }
        } catch (IOException e) {
            logger.error("Error loading properties file {}: {}", filename, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Determina si un locale es regional (ej: ES_MX, EN_US)
     */
    private boolean isRegionalLocale(LocalizationCodes locale) {
        return locale.name().contains("_");
    }
    
    /**
     * Obtiene el locale genérico para un locale regional
     * Ej: ES_MX -> ES, EN_US -> EN
     */
    private LocalizationCodes getGenericLocale(LocalizationCodes locale) {
        if (!isRegionalLocale(locale)) {
            return locale;
        }
        
        String genericName = locale.name().split("_")[0];
        try {
            return LocalizationCodes.valueOf(genericName);
        } catch (IllegalArgumentException e) {
            logger.warn("Could not find generic locale for: {}", locale);
            return LocalizationCodes.EN; // Último fallback
        }
    }

    /**
     * Obtiene el mensaje localizado para un tipo de notificación y idioma
     */
    public String getMessage(NotificationDataType type, LocalizationCodes locale) {
        return getNotificationTexts(type, locale).getMessage();
    }

    /**
     * Obtiene el título localizado para un tipo de notificación y idioma
     */
    public String getTitle(NotificationDataType type, LocalizationCodes locale) {
        return getNotificationTexts(type, locale).getTitle();
    }

    /**
     * Obtiene los textos de notificación con fallback automático
     */
    private NotificationTexts getNotificationTexts(NotificationDataType type, LocalizationCodes locale) {
        Map<LocalizationCodes, NotificationTexts> typeMessages = messages.get(type);
        if (typeMessages == null) {
            logger.warn("No messages found for notification type: {}", type);
            return getDefaultNotificationTexts();
        }
        
        NotificationTexts texts = typeMessages.get(locale);
        if (texts == null) {
            // Fallback al idioma por defecto
            texts = typeMessages.get(fallbackLocale);
            if (texts == null) {
                logger.warn("No fallback messages found for type: {} and locale: {}", type, fallbackLocale);
                return getDefaultNotificationTexts();
            }
            logger.debug("Using fallback locale {} for notification type: {}", fallbackLocale, type);
        }
        
        return texts;
    }

    /**
     * Retorna textos por defecto cuando no se encuentra ninguna traducción
     */
    private NotificationTexts getDefaultNotificationTexts() {
        return new NotificationTexts("New Notification", "You have a new notification!");
    }

}
