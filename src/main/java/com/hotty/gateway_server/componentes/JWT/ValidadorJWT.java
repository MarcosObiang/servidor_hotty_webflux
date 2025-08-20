package com.hotty.gateway_server.componentes.JWT;


import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class ValidadorJWT {

    @Value("${jwt.secret.key}")
    private String CLAVE_SECRETA;

    public boolean validarToken(String token) {
        byte[] claveDecodificada = Base64.getDecoder().decode(CLAVE_SECRETA);
        try {
            Claims claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor((claveDecodificada))).build()
                    .parseSignedClaims(token).getPayload();

            if (!claims.getExpiration().after(Date.from(Instant.now()))) {
                return false;
            }
            return true;

        } catch (Exception e) {

            System.out.println("Error al validar el token: " + e.getMessage());
            return false;
        }
    }

}
