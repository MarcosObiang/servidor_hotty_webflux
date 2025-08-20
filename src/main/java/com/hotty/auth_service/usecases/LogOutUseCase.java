package com.hotty.auth_service.usecases;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.hotty.auth_service.interfaces.AuthTokenDataModelRepository;

import reactor.core.publisher.Mono;

@Component
public class LogOutUseCase {

    private final AuthTokenDataModelRepository authTokenDataModelRepository;

    public LogOutUseCase(AuthTokenDataModelRepository authTokenDataModelRepository) {
        this.authTokenDataModelRepository = authTokenDataModelRepository;
    }

    /**
     * 
     * Revoca todos los tokens activos para el usuario especificado.
     * 
     * @param userUID El identificador único del usuario.
     * @return Un {@code Mono<Void>} que completa cuando todas las operaciones de revocación han sido iniciadas.
     */
    public Mono<Void> execute(String userUID) {
        return authTokenDataModelRepository.getAllActiveTokensByUserUID(userUID)
            .flatMap(token -> {
                Duration tokenRemainingLifetime = Duration.between(Instant.now(), token.getExpiresAt());
                // revokeActiveToken maneja internamente duraciones no positivas (ej. si el token ya expiró)
                // no añadiéndolo a Redis pero sí intentando la actualización en MongoDB.
                return authTokenDataModelRepository.revokeActiveToken(token.getTokenUID(), tokenRemainingLifetime);
            })
            .then(); // .then() convierte el Flux<Void> resultante de flatMap a un Mono<Void>
                     // que completa cuando todos los Monos internos (de revokeActiveToken) completan.
    }
}
