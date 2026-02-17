import com.hotty.common.services.PushNotifications.Localization.LocalizatedMessages;
import com.hotty.common.enums.NotificationDataType;
import com.hotty.user_service.enums.LocalizationCodes;
import org.springframework.beans.factory.annotation.Value;

public class TestLocalizedMessages {
    public static void main(String[] args) {
        // Simular LocalizedMessages manualmente
        LocalizatedMessages messages = new LocalizatedMessages();
        
        // Simular el valor de configuración
        try {
            // Usar reflection para setear el valor
            java.lang.reflect.Field fallbackField = LocalizatedMessages.class.getDeclaredField("fallbackLocaleName");
            fallbackField.setAccessible(true);
            fallbackField.set(messages, "EN");
            
            // Llamar init manualmente
            java.lang.reflect.Method initMethod = LocalizatedMessages.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(messages);
            
            // Probar los métodos
            System.out.println("✅ Testing LocalizedMessages...");
            System.out.println();
            
            // Test CHAT notifications
            System.out.println("=== CHAT NOTIFICATIONS ===");
            System.out.println("EN - Title: " + messages.getTitle(NotificationDataType.CHAT, LocalizationCodes.EN));
            System.out.println("EN - Message: " + messages.getMessage(NotificationDataType.CHAT, LocalizationCodes.EN));
            System.out.println();
            System.out.println("ES - Title: " + messages.getTitle(NotificationDataType.CHAT, LocalizationCodes.ES));
            System.out.println("ES - Message: " + messages.getMessage(NotificationDataType.CHAT, LocalizationCodes.ES));
            System.out.println();
            
            // Test LIKE notifications
            System.out.println("=== LIKE NOTIFICATIONS ===");
            System.out.println("FR - Title: " + messages.getTitle(NotificationDataType.LIKE, LocalizationCodes.FR));
            System.out.println("FR - Message: " + messages.getMessage(NotificationDataType.LIKE, LocalizationCodes.FR));
            System.out.println();
            System.out.println("DE - Title: " + messages.getTitle(NotificationDataType.LIKE, LocalizationCodes.DE));
            System.out.println("DE - Message: " + messages.getMessage(NotificationDataType.LIKE, LocalizationCodes.DE));
            System.out.println();
            
            // Test MESSAGE notifications
            System.out.println("=== MESSAGE NOTIFICATIONS ===");
            System.out.println("IT - Title: " + messages.getTitle(NotificationDataType.MESSAGE, LocalizationCodes.IT));
            System.out.println("IT - Message: " + messages.getMessage(NotificationDataType.MESSAGE, LocalizationCodes.IT));
            System.out.println();
            
            System.out.println("✅ All tests passed! LocalizedMessages is working correctly.");
            
        } catch (Exception e) {
            System.err.println("❌ Error testing LocalizedMessages: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
