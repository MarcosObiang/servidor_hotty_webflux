package com.hotty.gateway_server.componentes.JWT;


import java.util.Base64;
import java.util.List;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class DecodificarToken {

    @Value("${jwt.secret.key}")
    private String CLAVE_SECRETA;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(CLAVE_SECRETA);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser().verifyWith(this.secretKey).build();
    }

    // Extraer el nombre de usuario del token
    public Mono<String> getUsernameFromToken(String token) {
        return Mono.fromCallable(() -> {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.getSubject();
        });
    }

    // Extraer las autoridades del token
    public Mono<List<GrantedAuthority>> getAuthoritiesFromToken(String token) {
        return Mono.fromCallable(() -> {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            String rol = claims.get("rol", String.class); // Asume que 'rol' es un String
            // Si 'rol' pudiera ser una lista o tener un prefijo como "ROLE_", ajusta aquí.
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(rol);
            return List.of(authority);
        });
    }

    public Mono<String> getUserIdFromToken(String token) {
        return Mono.fromCallable(() -> {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.get("userUID", String.class);
        });
    }

    /**
     * Extrae todos los claims (payload) de un token JWT.
     *
     * @param token El token JWT.
     * @return Un {@code Mono} que emite los {@code Claims} del token.
     */
    public Mono<Claims> getAllClaimsFromToken(String token) {
        return Mono.fromCallable(() -> jwtParser.parseSignedClaims(token).getPayload());
    }

}
