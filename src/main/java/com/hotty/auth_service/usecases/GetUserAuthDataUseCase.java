package com.hotty.auth_service.usecases;

import org.springframework.stereotype.Service;

import com.hotty.auth_service.interfaces.AuthDataModelRepository;
import com.hotty.auth_service.models.AuthDataModel;

import reactor.core.publisher.Mono;

@Service
public class GetUserAuthDataUseCase {

    private final AuthDataModelRepository authDataModelRepository;

    public GetUserAuthDataUseCase(AuthDataModelRepository authDataModelRepository) {
        this.authDataModelRepository = authDataModelRepository;
    }

    /**
     * Obtiene los datos de autenticación de un usuario por su UID
     * Este método es útil para cachear datos antes de eliminación
     * 
     * @param userUID El UID del usuario
     * @return Los datos de autenticación del usuario
     */
    public Mono<AuthDataModel> execute(String userUID) {
        if (userUID == null || userUID.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("El UID de usuario no puede ser nulo o vacío"));
        }

        return authDataModelRepository.findUserByUID(userUID);
    }
}
