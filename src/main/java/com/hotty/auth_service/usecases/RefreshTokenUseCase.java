package com.hotty.auth_service.usecases;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.interfaces.Claim;
import com.hotty.auth_service.interfaces.AuthTokenDataModelRepository;
import com.hotty.auth_service.models.AuthTokenDataModel;
import com.hotty.auth_service.services.JWT.JWTService;
import com.hotty.common.services.EventPublishers.TokenRevocationEventPublisher;

import reactor.core.publisher.Mono;

@Component
public class RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCase.class);

    private final AuthTokenDataModelRepository authTokenDataRepository;
    private final JWTService jwtService;
    private final TokenRevocationEventPublisher tokenRevocationEventPublisher;

    public RefreshTokenUseCase(AuthTokenDataModelRepository authTokenDataRepository, JWTService jwtService,
            TokenRevocationEventPublisher tokenRevocationEventPublisher) {
        this.authTokenDataRepository = authTokenDataRepository;
        this.jwtService = jwtService;
        this.tokenRevocationEventPublisher = tokenRevocationEventPublisher;
    }

    public Mono<AuthTokenDataModel> execute(String refreshTokenUID) {
        return authTokenDataRepository.findByTokenUID(refreshTokenUID)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Refresh token no encontrado")))
            .flatMap(tokenData -> {
                // ✅ VALIDACIONES CORREGIDAS
                if (tokenData.isRevoked() || tokenData.isRefreshTokenRevoked()) {
                    return Mono.error(new IllegalArgumentException("Refresh token revocado: " + refreshTokenUID));
                }

                if (tokenData.getRefreshTokenExpiresAt() != null && tokenData.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
                    return Mono.error(new IllegalArgumentException("Refresh token expirado: " + refreshTokenUID));
                }

                // ✅ OBTENER CLAIMS DEL REFRESH TOKEN
                Map<String, Claim> refreshTokenClaims;
                try {
                    refreshTokenClaims = jwtService.getAllClaimsFromToken(tokenData.getRefreshToken());
                } catch (Exception e) {
                    return Mono.error(new IllegalArgumentException("Refresh token inválido o corrupto: " + refreshTokenUID));
                }

                // ✅ EXTRAER DATOS DE FORMA SEGURA
                String userName = getClaimValue(refreshTokenClaims, "userName");
                String userUID = getClaimValue(refreshTokenClaims, "userUID");
                String email = getClaimValue(refreshTokenClaims, "email"); // Obtener email real
                Boolean isRefreshToken = getClaimBooleanValue(refreshTokenClaims, "isRefreshToken");

                if (userName == null || userUID == null) {
                    return Mono.error(new IllegalArgumentException("Refresh token no contiene datos de usuario válidos"));
                }

                if (isRefreshToken == null || !isRefreshToken) {
                    return Mono.error(new IllegalArgumentException("El token proporcionado no es un refresh token válido"));
                }

                // ✅ GENERAR NUEVO ACCESS TOKEN (DE FORMA REACTIVA)
                return jwtService.generateToken(userName, email != null ? email : "email@example.com", userUID)
                    .flatMap(newTokenData -> {
                        // 🔔 NOTIFICAR REVOCACIÓN DEL ACCESS TOKEN ANTERIOR
                        // Crear una copia del token actual para notificar su revocación
                        AuthTokenDataModel previousTokenState = new AuthTokenDataModel();
                        previousTokenState.setTokenUID(tokenData.getTokenUID());
                        previousTokenState.setUserUID(tokenData.getUserUID());
                        previousTokenState.setToken(tokenData.getToken()); // Token anterior que será revocado
                        previousTokenState.setRefreshToken(tokenData.getRefreshToken());

                        // Notificar revocación del access token anterior (no bloqueante)
                        tokenRevocationEventPublisher.publishAccessTokenRevoked(previousTokenState)
                            .doOnError(e -> log.warn("Failed to publish access token revocation event for tokenUID: {}", 
                                tokenData.getTokenUID(), e))
                            .subscribe(); // Fire-and-forget

                        // ✅ ACTUALIZAR EL TOKEN EXISTENTE CON EL NUEVO ACCESS TOKEN
                        tokenData.setToken(newTokenData.getToken());
                        tokenData.setIssuedAt(newTokenData.getIssuedAt());
                        tokenData.setExpiresAt(newTokenData.getExpiresAt());
                        tokenData.setRevoked(false);
                        tokenData.setExpired(false);

                        // ✅ GUARDAR Y RETORNAR EL TOKEN ACTUALIZADO
                        return authTokenDataRepository.saveTokenToAuditLog(tokenData);
                    });
            });
    }

    // ✅ MÉTODOS HELPER PARA EXTRAER CLAIMS DE FORMA SEGURA
    private String getClaimValue(Map<String, Claim> claims, String key) {
        Claim claim = claims.get(key);
        return claim != null ? claim.asString() : null;
    }

    private Boolean getClaimBooleanValue(Map<String, Claim> claims, String key) {
        Claim claim = claims.get(key);
        return claim != null ? claim.asBoolean() : null;
    }
}
