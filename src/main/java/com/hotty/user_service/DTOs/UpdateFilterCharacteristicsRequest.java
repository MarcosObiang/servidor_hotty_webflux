package com.hotty.user_service.DTOs;

import com.hotty.user_service.model.UserCharacteristicsModel;
import lombok.Data;

@Data
public class UpdateFilterCharacteristicsRequest {
    private UserCharacteristicsModel characteristics;
    private Integer searchRadiusInKm;
    private Integer maxAge;
    private Integer minAge;
    private String sexPreference;
}