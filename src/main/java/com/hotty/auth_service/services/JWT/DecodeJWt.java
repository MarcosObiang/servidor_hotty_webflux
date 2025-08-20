package com.hotty.auth_service.services.JWT;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;


import java.net.URI;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class DecodeJWt {

    private static final String GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    public static DecodedJWT decodeAndVerifyGoogleIdToken(String idToken) throws Exception {
        // Decodificar sin verificar para obtener el "kid"
        DecodedJWT decodedJWT = JWT.decode(idToken);
        String keyId = decodedJWT.getKeyId();

        // Obtener la clave pública de Google
        JwkProvider provider = new JwkProviderBuilder(new URI(GOOGLE_CERTS_URL).toURL())
                .cached(10, 24, TimeUnit.HOURS) // Cachear claves para evitar peticiones repetidas
                .build();
        Jwk jwk = provider.get(keyId);
        RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

        // Verificar la firma del token
        return JWT.require(Algorithm.RSA256(publicKey, null))
                .build()
                .verify(idToken);
    }

}
