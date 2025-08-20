package com.hotty.auth_service.interfaces;


/**
 * Interfaz para una fábrica que proporciona la estrategia de autenticación
 * adecuada
 * basada en el identificador del proveedor.
 */
public interface AuthProviderStrategyFactory {

    /**
     * Recupera la estrategia de autenticación para el proveedor especificado.
     *
     * @param providerId El identificador único del proveedor de autenticación (ej.
     *                   "google", "facebook", "local").
     * @return La {@link AuthProviderStrategy} correspondiente al providerId.
     * @throws IllegalArgumentException si no se encuentra una estrategia para el
     *                                  providerId dado.
     */
    AuthProviderStrategy getStrategy(String providerId);
}