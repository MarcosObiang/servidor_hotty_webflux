package com.hotty.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsModel {

private Boolean isDarkModeEnabled = false;
private Boolean notificationsEnabled = true;
private Integer searchRadiusInKm = 20;
private Boolean isVisibleToOtherUsers = true;
private Integer minAge = 18;
private Integer maxAge = 100;
private String sexPreference;

    // El sexo del usuario (`userSex`) ya está definido en el nivel superior de UserDataModel (`sex`),
    // por lo que no es necesario duplicarlo aquí para evitar redundancia.
}
