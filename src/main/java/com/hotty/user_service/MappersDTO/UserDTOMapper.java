package com.hotty.user_service.MappersDTO;

import com.hotty.user_service.DTOs.UserDTO;
import com.hotty.user_service.model.UserDataModel;
import java.time.LocalDate;

public class UserDTOMapper {

    public static UserDTO toDTO(UserDataModel model) {
        if (model == null)
            return null;

        // Adjust the constructor call to match the actual UserDTO constructor
        return new UserDTO(
                model.getUserUID(),
                model.getName(),
                model.getUserImage1(),
                model.getUserImage2(),
                model.getUserImage3(),
                model.getUserImage4(),
                model.getUserImage5(),
                model.getUserImage6(),
                model.getSex(),
                model.getUserBio(),
                model.getBirthDate(),
                model.getCharacteristics() // Direct assignment as both are LocalDate
        );
    }

    public static UserDataModel toModel(UserDTO dto) {
        if (dto == null)
            return null;

        UserDataModel model = new UserDataModel();
        model.setUserUID(dto.getUserUID());
        model.setName(dto.getName());
        model.setUserImage1(dto.getUserImage1());
        model.setUserImage2(dto.getUserImage2());
        model.setUserImage3(dto.getUserImage3());
        model.setUserImage4(dto.getUserImage4());
        model.setUserImage5(dto.getUserImage5());
        model.setUserImage6(dto.getUserImage6());

        model.setSex(dto.getSex());
        model.setUserBio(dto.getUserBio());
        model.setBirthDate(dto.getBirthDate());
        model.setCharacteristics(dto.getCharacteristics()); // Direct assignment as both are LocalDate
        // No seteamos ni id, ni location, ni datingSexPreference
        return model;
    }

}
