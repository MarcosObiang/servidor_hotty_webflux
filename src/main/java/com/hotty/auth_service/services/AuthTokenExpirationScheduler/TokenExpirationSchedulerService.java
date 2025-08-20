package com.hotty.auth_service.services.AuthTokenExpirationScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
@Service
public class TokenExpirationSchedulerService {
    @Autowired
    private TaskScheduler scheduler;
    private TokenExpirationNotificationService tokenExpirationNotificationService;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public TokenExpirationSchedulerService(TaskScheduler scheduler,
            TokenExpirationNotificationService tokenExpirationNotificationService) {
        this.scheduler = scheduler;
        this.tokenExpirationNotificationService = tokenExpirationNotificationService;
    }

    public void scheduleTokenExpirationNotification(String userUID, Instant expirationTime,Runnable runnable) {
        long delay = Duration.between(Instant.now(), expirationTime).toMillis();

        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> {
            tokenExpirationNotificationService.notifyListeners(userUID);
            runnable.run();
            scheduledTasks.remove(userUID);

        }, Instant.now().plusMillis(delay));

        scheduledTasks.put(userUID, scheduledFuture);

    }

    public void cancelTokenExpirationNotification(String userUID) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(userUID);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

}
