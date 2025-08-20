package com.hotty.auth_service.usecases;

import org.springframework.stereotype.Service;

import com.hotty.auth_service.interfaces.AuthDataModelRepository;
import com.hotty.auth_service.models.AuthDataModel;

import reactor.core.publisher.Mono;

@Service
public class RestoreUserAuthDataUseCase {

    private final AuthDataModelRepository authDataModelRepository;

    public RestoreUserAuthDataUseCase(AuthDataModelRepository authDataModelRepository) {
        this.authDataModelRepository = authDataModelRepository;
    }

    /**
     * Restaura los datos de autenticación de un usuario
     * Este método es útil para hacer rollback después de una eliminación fallida
     * 
     * @param authDataModel Los datos de autenticación a restaurar
     * @return Los datos de autenticación restaurados
     */
    public Mono<AuthDataModel> execute(AuthDataModel authDataModel) {
        if (authDataModel == null) {
            return Mono.error(new IllegalArgumentException("Los datos de autenticación no pueden ser nulos"));
        }

        if (authDataModel.getUserUID() == null || authDataModel.getUserUID().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("El UID de usuario no puede ser nulo o vacío"));
        }

        if (authDataModel.getEmail() == null || authDataModel.getEmail().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("El email no puede ser nulo o vacío"));
        }

        // Guardar/restaurar los datos de autenticación
        return authDataModelRepository.save(authDataModel);
    }
}
