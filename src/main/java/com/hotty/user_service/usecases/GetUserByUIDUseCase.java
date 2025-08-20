package com.hotty.user_service.usecases;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hotty.user_service.DTOs.UserDTO;
import com.hotty.user_service.MappersDTO.UserDTOMapper;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import reactor.core.publisher.Mono;

@Component
public class GetUserByUIDUseCase {

    private UserModelRepository userModelRepository;

    public GetUserByUIDUseCase(UserModelRepository userModelRepository) {
        this.userModelRepository = userModelRepository;
    }

    public Mono<UserDTO> executeWithDTO(String userUID) {
        System.out.println("*****************************************************************userUID: " + userUID);
        return userModelRepository.findByUserUID(userUID).map(UserDTOMapper::toDTO);

    }

    public Mono<UserDataModel> execute(String userUID) {
        return userModelRepository.findByUserUID(userUID);
    }

}
