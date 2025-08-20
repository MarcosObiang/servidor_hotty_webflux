package com.hotty.user_service.DTOs;

import java.time.LocalDate;

import com.hotty.user_service.model.UserCharacteristicsModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTOwithDistance {

    private String userUID;
    private String name;
    private String userImage1;
    private String userImage2;
    private String userImage3;
    private String userImage4;
    private String userImage5;
    private String userImage6;
    private String sex;
    private String userBio;
    private LocalDate birthDate;
    private UserCharacteristicsModel characteristics;
    private double distance; // Distance in kilometers


}
