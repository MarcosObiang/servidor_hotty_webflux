package com.hotty.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.hotty.gateway_server.componentes.JWT.DecodificarToken;
import com.hotty.gateway_server.componentes.securityFilters.AddUserUidFilter;
import com.hotty.gateway_server.componentes.securityFilters.IsTokenValidFilter;

/**
 * Configuración de seguridad común para todo el monolito.
 * Integra la funcionalidad de autenticación del gateway con el resto de servicios.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public AddUserUidFilter addUserUIDFilter(DecodificarToken decodificarToken) {
        return new AddUserUidFilter(decodificarToken);
    }

    @Bean
    public IsTokenValidFilter isTokenValidFilter(DecodificarToken decodificarToken,
                                                 ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        return new IsTokenValidFilter(decodificarToken, reactiveRedisTemplate);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            IsTokenValidFilter isTokenValidFilter,
            AddUserUidFilter addUserUidFilter) {
        
        return http
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF para APIs REST
                .authorizeExchange(exchange -> exchange
                        // Rutas públicas del gateway (OAuth2 y autenticación)
                        .pathMatchers("/auth/*/callback/**").permitAll()
                        
                        // Rutas públicas específicas de servicios
                        .pathMatchers("/health", "/actuator/**").permitAll()

                        // Rutas públicas específicas de servicios
                        .pathMatchers("/subscriptions-service/webhooks/**").permitAll()

                        // Todas las demás rutas requieren autenticación
                        .anyExchange().authenticated()
                )
                // Filtros de seguridad en orden
                .addFilterBefore(isTokenValidFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(addUserUidFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                
                // Configuración OAuth2 si es necesaria
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth/authorization")
                )
                .build();
    }
}
