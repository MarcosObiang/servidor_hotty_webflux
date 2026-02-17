package com.hotty.auth_service.services.AuthProviders.Google;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers; // Importa Schedulers para ejecutar blocking ops en un pool separado

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

// Clase para representar la respuesta del endpoint /token de Google
record GoogleTokenResponse(String id_token, String access_token, String expires_in, String token_type) {
}

@Component
public class GoogleTokenManager {

    private final String googleClientIDString = System.getenv("GOOGLE_CLIENT_ID");
    private final String googleRedirectUriString = System.getenv("GOOGLE_REDIRECT_URI");

    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    private final WebClient webClient;
    private final JwkProvider jwkProvider;
    private final ObjectMapper objectMapper; // Para parsear JSON manualmente si es necesario

    public GoogleTokenManager(WebClient.Builder webClientBuilder, ObjectMapper objectMapper)
            throws MalformedURLException {
        this.webClient = webClientBuilder.baseUrl(GOOGLE_TOKEN_URI).build();
        this.objectMapper = objectMapper;

        // Configuración del JwkProvider para cachear las claves públicas de Google
        this.jwkProvider = new JwkProviderBuilder(URI.create(GOOGLE_CERTS_URL).toURL())
                .cached(10, 24, TimeUnit.HOURS) // Cachear 10 claves por 24 horas
                .build();
    }

    /**
     * Intercambia un código de autorización de Google por tokens de acceso e ID.
     * Realiza una llamada HTTP POST no bloqueante.
     *
     * @param code Código de autorización recibido de Google.
     * @return Mono que emite el DecodedJWT si el proceso es exitoso.
     */
    public Mono<DecodedJWT> getTokenFromCode(String code) {
        System.out.println("este es el code de google: " + code);
        System.out.println("Este es el client id de google: " + googleClientIDString);
        return webClient.post()
                .uri(GOOGLE_TOKEN_URI) // La URL completa para el POST
                .body(BodyInserters.fromFormData("client_id", googleClientIDString)
                        .with("code", code)
                        .with("grant_type", "authorization_code")
                        // Asegúrate de que este redirect_uri coincide EXACTAMENTE con el usado en el
                        // paso de autorización inicial
                        .with("redirect_uri", googleRedirectUriString)) // Usar la propiedad inyectada
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class) // Mapear la respuesta directamente a un Pojo
                .flatMap(tokenResponse -> {
                    if (tokenResponse == null || tokenResponse.id_token() == null) {
                        return Mono.error(
                                new IllegalArgumentException("Invalid Google token response: missing ID token."));
                    }
                    return decodeAndVerifyGoogleIdToken(tokenResponse.id_token());
                })
                .onErrorMap(e -> {
                    // Mapear excepciones de WebClient a algo más específico si es necesario
                    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException wcException = (org.springframework.web.reactive.function.client.WebClientResponseException) e;
                        System.err.println(
                                "ERROR 400 de Google Token Exchange (WebClient): " + wcException.getStatusCode());
                        System.err.println(
                                "Cuerpo de respuesta de error de Google: " + wcException.getResponseBodyAsString()); // <<<
                                                                                                                     // ¡Esto
                                                                                                                     // mostrará
                                                                                                                     // el
                                                                                                                     // mensaje
                                                                                                                     // de
                                                                                                                     // Google!
                        System.err.println("Encabezados de respuesta de error: " + wcException.getHeaders());
                        return new RuntimeException("Google token exchange failed: " + wcException.getStatusCode()
                                + " - " + wcException.getResponseBodyAsString(), e);
                    }
                    return new RuntimeException("Error exchanging Google code for token: " + e.getMessage(), e);
                });
    }

    /**
     * Decodifica y verifica la firma y expiración de un ID Token de Google.
     * Esta operación es potencialmente bloqueante debido a la descarga de JWKs y
     * verificación criptográfica,
     * por lo que se ejecuta en un Scheduler separado.
     *
     * @param idToken El ID Token JWT de Google.
     * @return Mono que emite el DecodedJWT verificado.
     */
    private Mono<DecodedJWT> decodeAndVerifyGoogleIdToken(String idToken) {
        return Mono.fromCallable(() -> {
            // Decodificar el JWT sin verificar la firma para obtener el "kid" (key ID)
            DecodedJWT decodedJWT = JWT.decode(idToken);
            String keyId = decodedJWT.getKeyId();

            if (keyId == null) {
                throw new JWTVerificationException("Google ID Token missing kid header.");
            }

            // Obtener la clave pública correspondiente del JwkProvider (puede ser blocking
            // si no está en caché)
            Jwk jwk = jwkProvider.get(keyId);
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            // Verificar la firma del token con la clave pública obtenida
            return JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer("https://accounts.google.com") // Asegura que el emisor sea Google
                    // Puedes añadir más validaciones de claims aquí, ej:
                    // .withAudience(googleClientIDString) // Validar el 'aud' (audience)
                    .build()
                    .verify(idToken);
        })
                // Suscribirse en un scheduler elástico para no bloquear el hilo principal
                // (Netty)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> {
                    // Mapear excepciones de verificación JWT a un tipo de excepción más adecuado
                    if (e instanceof JwkException) {
                        return new RuntimeException(
                                "Failed to retrieve Google public key for JWT verification: " + e.getMessage(), e);
                    }
                    if (e instanceof JWTVerificationException) {
                        return new RuntimeException("Google ID Token verification failed: " + e.getMessage(), e);
                    }
                    return new RuntimeException(
                            "Unexpected error during Google ID Token verification: " + e.getMessage(), e);
                });
    }
}