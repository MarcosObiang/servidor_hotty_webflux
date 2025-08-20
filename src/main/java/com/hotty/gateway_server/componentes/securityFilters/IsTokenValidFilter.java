package com.hotty.gateway_server.componentes.securityFilters;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.hotty.gateway_server.componentes.JWT.DecodificarToken;
import com.hotty.gateway_server.exceptions.UnauthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import reactor.core.publisher.Mono;

public class IsTokenValidFilter implements WebFilter, Ordered {

    private DecodificarToken decodificarToken;
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public IsTokenValidFilter(DecodificarToken decodificarToken, ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.decodificarToken = decodificarToken;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Considera hacer esta lista de rutas excluidas configurable
        String path = exchange.getRequest().getURI().getPath();
        if ((path.startsWith("/auth/") && path.endsWith("/callback")) || path.startsWith("/oauth/") || path.startsWith("/subscriptions-service/")) {
            // Salta el filtro para esta ruta
            return chain.filter(exchange);
        }

        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .filter(authHeader -> authHeader.startsWith("Bearer "))
            .map(authHeader -> authHeader.substring(7))
            .switchIfEmpty(Mono.error(new UnauthorizedException("Authorization header is missing, invalid, or not Bearer type")))
            .flatMap(this::validateTokenAndBuildAuthentication)
            .flatMap(authentication -> {
                SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
                System.out.println("---------------------------Token validado---------------------------");
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
            })
            // Manejar errores específicos de JWT y otros errores generales
            .onErrorResume(ExpiredJwtException.class, e -> Mono.error(new UnauthorizedException("Token expired: " + e.getMessage())))
            .onErrorResume(SignatureException.class, e -> Mono.error(new UnauthorizedException("Invalid token signature: " + e.getMessage())))
            .onErrorResume(UnauthorizedException.class, e -> Mono.error(e)) // Re-throw nuestras UnauthorizedExceptions
            .onErrorResume(e -> Mono.error(new UnauthorizedException( e.getMessage()))); // Envolver otras excepciones
    }

    private Mono<Authentication> validateTokenAndBuildAuthentication(String token) {
        return decodificarToken.getAllClaimsFromToken(token)
            .flatMap(claims -> {
                Instant expirationTime = claims.getExpiration().toInstant();
                String tokenUID = claims.get("tokenUID", String.class);

                if (tokenUID == null) {
                    return Mono.error(new UnauthorizedException("Token UID (jti) missing from token claims."));
                }

                // Primero, verificar si el token está en la blacklist de Redis
                if (expirationTime.isBefore(Instant.now())) {
                    return Mono.error(new ExpiredJwtException(null, claims, "Token has expired"));
                }

                // Extraer userUID y roles del token
                // Asumimos que 'userUID' está en los claims y 'rol' también.
                String userUID = claims.get("userUID", String.class);
                // String roleClaim = claims.get("rol", String.class); // Asume un solo rol como String
                // List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleClaim));

                // Podrías usar decodificarToken.getUserIdFromToken(token) y decodificarToken.getAuthoritiesFromToken(token)
                // si prefieres mantener esa lógica separada, pero ya tenemos los claims aquí.
                // return Mono.just(new UsernamePasswordAuthenticationToken(userUID, null, authorities));
                return reactiveRedisTemplate.hasKey(tokenUID)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            return Mono.error(new UnauthorizedException("Token has been revoked."));
                        }
                        // Si no está en blacklist y no ha expirado, procede a crear la autenticación
                        String roleClaim = claims.get("rol", String.class); // Asume un solo rol como String
                        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleClaim));
                        return Mono.just(new UsernamePasswordAuthenticationToken(userUID, null, authorities));
                    });
            });
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.AUTHENTICATION.getOrder();
    }
    }

