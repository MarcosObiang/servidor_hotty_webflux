package com.hotty.auth_service.usecases;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotty.auth_service.interfaces.AuthTokenDataModelRepository;
import com.hotty.common.services.EventPublishers.TokenRevocationEventPublisher;

import reactor.core.publisher.Mono;

@Component
public class LogOutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogOutUseCase.class);

    private final AuthTokenDataModelRepository authTokenDataModelRepository;
    private final TokenRevocationEventPublisher tokenRevocationEventPublisher;

    public LogOutUseCase(AuthTokenDataModelRepository authTokenDataModelRepository,
            TokenRevocationEventPublisher tokenRevocationEventPublisher) {
        this.authTokenDataModelRepository = authTokenDataModelRepository;
        this.tokenRevocationEventPublisher = tokenRevocationEventPublisher;
    }

    /**
     * 
     * Revoca todos los tokens activos para el usuario especificado y notifica al servicio de tiempo real.
     * 
     * @param userUID El identificador único del usuario.
     * @return Un {@code Mono<Void>} que completa cuando todas las operaciones de revocación han sido iniciadas.
     */
    public Mono<Void> execute(String userUID) {
        return authTokenDataModelRepository.getAllActiveTokensByUserUID(userUID)
            .flatMap(token -> {
                // 🔔 NOTIFICAR REVOCACIÓN DE SESIÓN COMPLETA (no bloqueante)
                tokenRevocationEventPublisher.publishSessionRevoked(token)
                    .doOnError(e -> log.warn("Failed to publish session revocation event for tokenUID: {} userUID: {}", 
                        token.getTokenUID(), userUID, e))
                    .subscribe(); // Fire-and-forget

                Duration tokenRemainingLifetime = Duration.between(Instant.now(), token.getExpiresAt());
                // revokeActiveToken maneja internamente duraciones no positivas (ej. si el token ya expiró)
                // no añadiéndolo a Redis pero sí intentando la actualización en MongoDB.
                return authTokenDataModelRepository.revokeActiveToken(token.getTokenUID(), tokenRemainingLifetime);
            })
            .then(); // .then() convierte el Flux<Void> resultante de flatMap a un Mono<Void>
                     // que completa cuando todos los Monos internos (de revokeActiveToken) completan.
    }
}
