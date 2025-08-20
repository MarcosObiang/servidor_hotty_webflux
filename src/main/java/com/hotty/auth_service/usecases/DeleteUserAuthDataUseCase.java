package com.hotty.auth_service.usecases;

import org.springframework.stereotype.Service;

import com.hotty.auth_service.interfaces.AuthDataModelRepository;

import reactor.core.publisher.Mono;

@Service
public class DeleteUserAuthDataUseCase {

    private final AuthDataModelRepository authDataModelRepository;

    public DeleteUserAuthDataUseCase(AuthDataModelRepository authDataModelRepository) {
        this.authDataModelRepository = authDataModelRepository;
    }

    /**
     * Deletes the authentication data for a user identified by their UID.
     *
     * @param userUID The unique identifier of the user whose authentication data is to be deleted.
     * @return A Mono that completes when the deletion is successful.
     */
    public Mono<Void> execute(String userUID) {
        if (userUID == null || userUID.isBlank()) {
            return Mono.error(new IllegalArgumentException("User UID cannot be null or blank."));
        }
        return authDataModelRepository.deleteByUserUID(userUID);
    }
    
}
