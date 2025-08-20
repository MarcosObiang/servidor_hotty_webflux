package com.hotty.auth_service.interfaces;

import com.hotty.auth_service.models.AuthTokenDataModel;

import reactor.core.publisher.Mono;

public interface AuthProviderStrategy {
    

 // Identificador único para cada estrategia (ej. "google", "facebook", "email_password")
    String getProviderId();

    /**
     * Autentica o registra un usuario utilizando un token/código de un tercero.
     * Retorna el token interno generado por tu aplicación.
     *
     * @param thirdPartyToken El token o código de autorización del proveedor externo.
     * @return Mono que emite el AuthTokenDataModel interno generado.
     */
    Mono<AuthTokenDataModel> authenticate(String thirdPartyToken);
}
