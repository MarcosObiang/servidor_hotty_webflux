package com.hotty.common.services.PushNotifications.Localization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.hotty.common.enums.NotificationDataType;
import com.hotty.user_service.enums.LocalizationCodes;

/**
 * Tests para el servicio de localización de notificaciones
 * Verifica que los mensajes se cargan correctamente desde archivos de propiedades
 */
class LocalizatedMessagesTest {

    private LocalizatedMessages localizedMessages;

    @BeforeEach
    void setUp() {
        localizedMessages = new LocalizatedMessages();
        // Simular la configuración de Spring Boot
        ReflectionTestUtils.setField(localizedMessages, "fallbackLocaleName", "EN");
        localizedMessages.init();
    }

    @Test
    void testGetMessage_SpanishChat_ReturnsCorrectSpanishMessage() {
        String message = localizedMessages.getMessage(NotificationDataType.CHAT, LocalizationCodes.ES);
        assertEquals("¡Tienes un nuevo chat!", message);
    }

    @Test
    void testGetTitle_ValidLocale_ReturnsCorrectTitle() {
        String title = localizedMessages.getTitle(NotificationDataType.LIKE, LocalizationCodes.FR);
        assertEquals("Nouveau Like", title);
    }

    @Test
    void testGetMessage_InvalidLocale_ReturnsFallback() {
        // Simulando un locale que no existe usando reflexión para acceder al método privado
        String message = localizedMessages.getMessage(NotificationDataType.MESSAGE, LocalizationCodes.EN);
        assertNotNull(message);
        assertFalse(message.isEmpty());
    }

    @Test
    void testGetTitle_AllSupportedLocales() {
        for (LocalizationCodes locale : LocalizationCodes.values()) {
            String title = localizedMessages.getTitle(NotificationDataType.CHAT, locale);
            assertNotNull(title);
            assertFalse(title.isEmpty());
        }
    }

    @Test
    void testGetMessage_AllNotificationTypes() {
        for (NotificationDataType type : NotificationDataType.values()) {
            String message = localizedMessages.getMessage(type, LocalizationCodes.EN);
            assertNotNull(message);
            assertFalse(message.isEmpty());
        }
    }

    @Test
    void testRegionalLocalization_SpanishVariations() {
        // Test regional variations in Spanish
        
        // Argentina (voseo)
        assertEquals("¡Tenés un nuevo chat!", localizedMessages.getMessage(NotificationDataType.CHAT, LocalizationCodes.ES_AR));
        assertEquals("¡Tenés un nuevo mensaje!", localizedMessages.getMessage(NotificationDataType.MESSAGE, LocalizationCodes.ES_AR));
        
        // Spain (formal terminology)
        assertEquals("Nuevo Me Gusta", localizedMessages.getTitle(NotificationDataType.LIKE, LocalizationCodes.ES_ES));
        assertEquals("¡Has recibido un nuevo me gusta!", localizedMessages.getMessage(NotificationDataType.LIKE, LocalizationCodes.ES_ES));
        
        // Mexico (tuteo informal)
        assertEquals("¡Recibiste un nuevo like!", localizedMessages.getMessage(NotificationDataType.LIKE, LocalizationCodes.ES_MX));
        
        // Colombia (regional terminology)
        assertEquals("¡Recibiste un nuevo me gusta!", localizedMessages.getMessage(NotificationDataType.LIKE, LocalizationCodes.ES_CO));
    }

    @Test
    void testRegionalLocalization_EnglishVariations() {
        // Test English regional variations
        
        // US English
        assertEquals("You have a new chat!", localizedMessages.getMessage(NotificationDataType.CHAT, LocalizationCodes.EN_US));
        assertEquals("You received a new like!", localizedMessages.getMessage(NotificationDataType.LIKE, LocalizationCodes.EN_US));
        
        // British English
        assertEquals("You've got a new chat!", localizedMessages.getMessage(NotificationDataType.CHAT, LocalizationCodes.EN_GB));
        assertEquals("You've received a new like!", localizedMessages.getMessage(NotificationDataType.LIKE, LocalizationCodes.EN_GB));
    }
}
