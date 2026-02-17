package com.hotty.user_service.model;

import com.hotty.common.enums.NotificationProvider;
import com.hotty.user_service.enums.LocalizationCodes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationDataModel {

    private String notificationToken;
    private NotificationProvider provider;
    private LocalizationCodes locale = LocalizationCodes.EN;

}
