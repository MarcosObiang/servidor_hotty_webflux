package com.hotty.common.services.PushNotifications.Factories;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotty.common.enums.NotificationProvider;
import com.hotty.common.interfaces.NotificationStrategy;
import com.hotty.common.services.PushNotifications.Strategies.FCMStrategy;

@Component
public class NotificationStrategyFactory {

    private final Map<NotificationProvider, NotificationStrategy> strategies = new HashMap<>();

    public NotificationStrategyFactory(NotificationStrategy fcmStrategy) {
        strategies.put(NotificationProvider.ANDROID, fcmStrategy);
    }

    public NotificationStrategy getStrategy(NotificationProvider provider) {
        return strategies.get(provider);
    }
}
