package com.hotty.auth_service.usecases;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotty.auth_service.interfaces.AuthTokenDataModelRepository;
import com.hotty.common.services.EventPublishers.TokenRevocationEventPublisher;

import reactor.core.publisher.Mono;

/**
 * Use case para revocar tokens por motivos de seguridad.
 * 
 * Este use case se puede usar en escenarios como:
 * - Detección de actividad sospechosa
 * - Compromiso de cuenta detectado
 * - Violación de políticas de seguridad
 * - Solicitud manual de administrador
 */
@Component
public class RevokeTokenForSecurityUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevokeTokenForSecurityUseCase.class);

    private final AuthTokenDataModelRepository authTokenDataRepository;
    private final TokenRevocationEventPublisher tokenRevocationEventPublisher;

    public RevokeTokenForSecurityUseCase(AuthTokenDataModelRepository authTokenDataRepository,
            TokenRevocationEventPublisher tokenRevocationEventPublisher) {
        this.authTokenDataRepository = authTokenDataRepository;
        this.tokenRevocationEventPublisher = tokenRevocationEventPublisher;
    }

    /**
     * Revoca un token específico por motivos de seguridad.
     * 
     * @param tokenUID El UID del token a revocar
     * @param reason La razón específica de la revocación por seguridad
     * @return Mono<Void> que se completa cuando el token ha sido revocado y notificado
     */
    public Mono<Void> execute(String tokenUID, String reason) {
        return authTokenDataRepository.findByTokenUID(tokenUID)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Token no encontrado: " + tokenUID)))
            .flatMap(tokenData -> {
                // Verificar si ya está revocado
                if (tokenData.isRevoked() && tokenData.isRefreshTokenRevoked()) {
                    log.info("Token {} ya estaba completamente revocado", tokenUID);
                    return Mono.empty();
                }

                // 🔔 NOTIFICAR REVOCACIÓN POR SEGURIDAD ANTES de revocar
                return tokenRevocationEventPublisher.publishTokenRevokedForSecurity(tokenData, reason)
                    .doOnError(e -> log.warn("Failed to publish security revocation event for tokenUID: {}", tokenUID, e))
                    .then(Mono.defer(() -> {
                        // ✅ REVOCAR LA SESIÓN COMPLETA
                        tokenData.revokeSession();
                        
                        // ✅ GUARDAR EL TOKEN REVOCADO
                        return authTokenDataRepository.saveTokenToAuditLog(tokenData);
                    }))
                    .doOnSuccess(savedToken -> log.info("Token {} revoked for security reason: {}", tokenUID, reason))
                    .then();
            });
    }

    /**
     * Revoca todos los tokens activos de un usuario por motivos de seguridad.
     * 
     * @param userUID El UID del usuario cuyas sesiones se van a revocar
     * @param reason La razón específica de la revocación por seguridad
     * @return Mono<Void> que se completa cuando todos los tokens han sido revocados y notificados
     */
    public Mono<Void> revokeAllUserTokensForSecurity(String userUID, String reason) {
        return authTokenDataRepository.getAllActiveTokensByUserUID(userUID)
            .flatMap(tokenData -> {
                // 🔔 NOTIFICAR REVOCACIÓN POR SEGURIDAD
                return tokenRevocationEventPublisher.publishTokenRevokedForSecurity(tokenData, reason)
                    .doOnError(e -> log.warn("Failed to publish security revocation event for tokenUID: {} userUID: {}", 
                        tokenData.getTokenUID(), userUID, e))
                    .then(Mono.defer(() -> {
                        // ✅ REVOCAR LA SESIÓN COMPLETA
                        tokenData.revokeSession();
                        
                        // ✅ GUARDAR EL TOKEN REVOCADO
                        return authTokenDataRepository.saveTokenToAuditLog(tokenData);
                    }));
            })
            .doOnNext(savedToken -> log.info("Token {} revoked for user {} - security reason: {}", 
                savedToken.getTokenUID(), userUID, reason))
            .then()
            .doOnSuccess(v -> log.info("All active tokens for user {} revoked for security reason: {}", userUID, reason));
    }
}
