package com.hotty.auth_service.Factory;


import org.springframework.stereotype.Component;

import com.hotty.auth_service.interfaces.AuthProviderStrategy;
import com.hotty.auth_service.interfaces.AuthProviderStrategyFactory;


import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementación de la fábrica que gestiona y proporciona las diferentes
 * estrategias de autenticación.
 * Descubre automáticamente todas las implementaciones de {@link AuthProviderStrategy}
 * y las mapea por su {@code providerId}.
 */
@Component
public class AuthProviderStrategyFactoryImpl implements AuthProviderStrategyFactory {

    // Un mapa para almacenar las estrategias, donde la clave es el providerId y el valor es la estrategia.
    private final Map<String, AuthProviderStrategy> strategies;

    /**
     * Constructor que inyecta todas las implementaciones de {@link AuthProviderStrategy}
     * encontradas en el contexto de Spring y las mapea por su providerId.
     *
     * @param strategyList Una lista de todas las implementaciones de AuthProviderStrategy.
     */
    public AuthProviderStrategyFactoryImpl(List<AuthProviderStrategy> strategyList) {
        // Recorre la lista de estrategias y las guarda en un mapa,
        // usando el getProviderId() de cada estrategia como clave.
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(AuthProviderStrategy::getProviderId, Function.identity()));
    }

    @Override
    public AuthProviderStrategy getStrategy(String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del proveedor no puede ser nulo o vacío.");
        }

        AuthProviderStrategy strategy = strategies.get(providerId);
        if (strategy == null) {
            throw new IllegalArgumentException("No se encontró ninguna estrategia de autenticación para el proveedor: " + providerId);
        }
        return strategy;
    }
}