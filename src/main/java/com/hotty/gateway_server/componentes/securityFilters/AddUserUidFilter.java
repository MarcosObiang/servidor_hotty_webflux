package com.hotty.gateway_server.componentes.securityFilters;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.hotty.gateway_server.componentes.JWT.DecodificarToken;
import com.hotty.gateway_server.exceptions.UnauthorizedException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import reactor.core.publisher.Mono;

public class AddUserUidFilter implements WebFilter, Ordered {

    private final DecodificarToken decodificarToken;

    // Inyección por constructor
    public AddUserUidFilter(DecodificarToken decodificarToken) {
        this.decodificarToken = decodificarToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // La exclusión de rutas como /auth/google/code** se maneja mejor en SecurityConfig.
        // Si este filtro se aplica a todas las rutas y necesitas excluir algunas aquí,
        // considera una lista configurable de patrones o un AntPathMatcher.
        if (exchange.getRequest().getURI().getPath().contains("/auth/") && exchange.getRequest().getURI().getPath().endsWith("/callback") || exchange.getRequest().getURI().getPath().startsWith("/oauth/") || exchange.getRequest().getURI().getPath().startsWith("/subscriptions-service/webhooks/")) {
            return chain.filter(exchange);
        }

        // Obtener el encabezado de autorización de forma reactiva
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                // Validar que el encabezado existe y empieza con "Bearer "
                .switchIfEmpty(Mono.error(new UnauthorizedException("Missing Authorization header")))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid Authorization header format")))
                // Extraer el token
                .map(authHeader -> authHeader.substring(7))
                // Decodificar el token para obtener el userUID de forma reactiva
                .flatMap(token -> decodificarToken.getUserIdFromToken(token))
                // Modificar la solicitud para añadir el userUID como encabezado
                .flatMap(userUID -> {
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("userUID", userUID)
                            .build();
                    // Continuar la cadena de filtros con la solicitud modificada
                     System.out.println(request.getHeaders().getFirst("userUID") + " Clave añadida con existo-----------------------------------------------------------"); // Considera usar un logger
                    return chain.filter(exchange.mutate().request(request).build()).onErrorResume(java.net.ConnectException.class, Mono::error); // Las excepciones de red aquí se propagarán
                })
                // Manejar errores específicos de JWT de forma reactiva
                .onErrorResume(ExpiredJwtException.class, ex -> Mono.error(new UnauthorizedException("Token expired in AddUserUidFilter: " + ex.getMessage(), ex)))
                .onErrorResume(SignatureException.class, ex -> Mono.error(new UnauthorizedException("Invalid token signature in AddUserUidFilter: " + ex.getMessage(), ex)))
                .onErrorResume(UnauthorizedException.class, Mono::error) // Re-lanza nuestras propias UnauthorizedExceptions o las de decodificarToken
                // Captura otras excepciones que puedan ocurrir ANTES de llamar a chain.filter o durante la decodificación del token,
                // pero NO errores de red de chain.filter.
                .onErrorResume(Throwable.class, e -> { // Cambiado a Throwable para capturar cualquier error
                    System.out.println("Error en AddUserUidFilter: " + e.getMessage()+" " +e.getClass());
                    // Evitar envolver errores de conexión de red como UnauthorizedException
                    // Ahora también verificamos io.netty.channel.AbstractChannel.AnnotatedConnectException
                    if (e instanceof java.net.ConnectException || 
                        (e.getCause() instanceof java.net.ConnectException) ) {
                        return Mono.error(e); // Propagar error de red para que lo maneje GlobalWebExceptionHandler como 5xx
                    }
                    return Mono.error(new UnauthorizedException("Unexpected error during token processing in AddUserUidFilter: " + e.getMessage(), e));
                });
    }

    @Override
    public int getOrder() {
        // Ejecutar después de IsTokenValidFilter
        return SecurityWebFiltersOrder.AUTHENTICATION.getOrder() + 1;
    }

}
