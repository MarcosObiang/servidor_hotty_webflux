package com.hotty.common.interfaces;

import com.hotty.common.enums.NotificationDataType;
import com.hotty.user_service.model.UserNotificationDataModel;

import reactor.core.publisher.Mono;


public interface NotificationStrategy {

    Mono<Void> sendNotification(NotificationDataType type, UserNotificationDataModel notificationDataModel);

}
