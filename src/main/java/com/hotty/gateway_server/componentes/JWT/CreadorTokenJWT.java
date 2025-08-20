package com.hotty.gateway_server.componentes.JWT;



import java.security.Signature;
import java.time.Instant;
import java.util.Date;
import java.util.Base64;
import java.util.Base64.Decoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class CreadorTokenJWT {

  @Value("${jwt.secret.key}")
  private String CLAVE_SECRETA;

  public String generarToken(Authentication autentication) {

    

    Decoder decoder = Base64.getDecoder();

    byte[] claveDecodificada = decoder.decode(CLAVE_SECRETA);

    return Jwts.builder().subject(autentication.getName()).issuedAt(new Date())
        .expiration(Date.from(Instant.now().plusSeconds(8640000))).signWith(Keys.hmacShaKeyFor(claveDecodificada))
        .compact();

  }
}
