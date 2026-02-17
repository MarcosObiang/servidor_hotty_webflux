package com.hotty.auth_service.interfaces;

import java.time.Duration;
import java.util.NoSuchElementException;

import com.hotty.auth_service.models.AuthTokenDataModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuthTokenDataModelRepository {

    /**
     * Recupera un único token de autenticación activo (no revocado) para un usuario
     * dado desde MongoDB.
     * Este método está pensado principalmente para operaciones de gestión o
     * limpieza específicas,
     * como invalidar sesiones anteriores cuando un usuario inicia sesión o cambia
     * su contraseña.
     * Es crucial entender que este método **NO** está diseñado para la validación
     * en tiempo real
     * de cada solicitud API entrante.
     *
     * @param userUID El identificador único del usuario cuyo token activo se desea
     *                recuperar.
     * @return Un {@code Mono} que emite el {@code AuthTokenDataModel} si se
     *         encuentra un token activo.
     *         Si existen múltiples tokens activos para el usuario, se devolverá el
     *         primero que encuentre MongoDB.
     *         Si no se encuentra ningún token activo para el usuario especificado,
     *         el {@code Mono} completará con un error
     *         {@code NoSuchElementException}.
     * @throws IllegalArgumentException si el {@code userUID} proporcionado es nulo
     *                                  o está vacío.
     * @throws NoSuchElementException   si no se encuentra ningún token activo para
     *                                  el {@code userUID} dado.
     */

    Flux<AuthTokenDataModel> getAllActiveTokensByUserUID(String userUID);

    Mono<AuthTokenDataModel> updateTokenRevokedStatusInAuditLog(String tokenUID, boolean isRevoked);

    Mono<AuthTokenDataModel> saveTokenToAuditLog(AuthTokenDataModel token);

    Mono<AuthTokenDataModel> findByTokenUID(String tokenUID);

    Mono<Void> revokeActiveToken(String tokenUID, Duration tokenRemainingLifetimeDuration);

}
