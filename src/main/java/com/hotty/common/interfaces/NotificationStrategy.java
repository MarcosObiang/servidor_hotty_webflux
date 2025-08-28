package com.hotty.common.interfaces;

import com.hotty.common.enums.NotificationDataType;

import reactor.core.publisher.Mono;


public interface NotificationStrategy {

    Mono<Void> sendNotification(String userId, NotificationDataType type);

}
