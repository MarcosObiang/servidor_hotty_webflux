package com.hotty.auth_service.services.JWT;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.hotty.auth_service.models.AuthTokenDataModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

// Asegúrate de que esta sea la ruta correcta a tu modelo
// import com.yourpackage.model.AuthTokenDataModel;

@Service
public class JWTService {

    @Value("${jwt.secret.key}")
    private String secretKey;
    // Considera usar Duration para mayor claridad y seguridad de tipos
    private static final long MAX_TOKEN_DURATION_SECONDS = 3600; // 1 hora en segundos
    private static final long MAX_REFRESH_TOKEN_DURATION_SECONDS = 604800; // 7 días en segundos (o la duración que
                                                                           // necesites)

    /**
     * Genera un nuevo Access Token JWT de forma reactiva y construye su modelo de
     * datos asociado.
     *
     * @param userName El nombre de usuario a incluir en el token.
     * @param email    El email del usuario a incluir en el token.
     * @param userUID  El UID único del usuario, utilizado como identificador
     *                 principal.
     * @return Un {@code Mono} que emite el {@code AuthTokenDataModel} conteniendo
     *         el token JWT generado y sus metadatos.
     */
    public Mono<AuthTokenDataModel> generateToken(String userName, String email, String userUID) {
        return Mono.fromCallable(() -> { // Envuelve la lógica síncrona en Mono.fromCallable
            Decoder decoder = Base64.getDecoder();
            byte[] claveDecodificada = decoder.decode(secretKey);

            String tokenUID = UUID.randomUUID().toString(); // Genera un UID único para este token

            ZonedDateTime issuedAt = ZonedDateTime.now();
            ZonedDateTime expirationDate = ZonedDateTime.now().plusSeconds(MAX_TOKEN_DURATION_SECONDS);
            ZonedDateTime refreshTokenExpirationDate = ZonedDateTime.now()
                    .plusSeconds(MAX_REFRESH_TOKEN_DURATION_SECONDS);

            String token = JWT.create()
                    .withClaim("userName", userName)
                    .withSubject(userName)
                    .withClaim("userUID", userUID)
                    .withClaim("isRefreshToken", false)
                    .withClaim("email", email)
                    .withIssuedAt(Date.from(issuedAt.toInstant()))
                    .withClaim("rol", "ROLE_USER") // Añadir el claim 'rol' con un valor por defecto
                    .withClaim("tokenUID", tokenUID) // Usa el tokenUID generado
                    .withExpiresAt(Date.from(expirationDate.toInstant()))
                    .sign(Algorithm.HMAC256(claveDecodificada));

            String refreshToken = JWT.create()
                    .withClaim("userName", userName)
                    .withSubject(userName)
                    .withClaim("userUID", userUID)
                    .withClaim("isRefreshToken", true)
                    .withClaim("email", email)

                    .withClaim("rol", "ROLE_USER") // Añadir el claim 'rol' también al Refresh Token si es necesario
                    .withIssuedAt(Date.from(issuedAt.toInstant()))
                    .withClaim("tokenUID", tokenUID)
                    .withExpiresAt(Date.from(refreshTokenExpirationDate.toInstant()))
                    .sign(Algorithm.HMAC256(claveDecodificada));

            AuthTokenDataModel authTokenDataModel = new AuthTokenDataModel();
            authTokenDataModel.setToken(token);
            authTokenDataModel.setRefreshToken(refreshToken);
            authTokenDataModel.setTokenUID(tokenUID);
            authTokenDataModel.setUserUID(userUID);
            authTokenDataModel.setIssuedAt(issuedAt.toInstant());
            authTokenDataModel.setExpiresAt(expirationDate.toInstant());
            authTokenDataModel.setRefreshTokenExpiresAt(refreshTokenExpirationDate.toInstant());

            return authTokenDataModel;
        });
    }

    /**
     * Obtiene todos los claims del token como un Map
     * 
     * @param token El JWT del cual extraer los claims
     *              Claims incluidos:
     * 
     *              String sub: Nombre de usuario
     *              String tokenUID: UID único del token
     *              Boolean isRefreshToken: Indica si es un refresh token
     *              String rol: Rol del usuario
     * 
     * @return Map con todos los claims del token
     * 
     */
    public Map<String, Claim> getAllClaimsFromToken(String token) {
        Decoder decoder = Base64.getDecoder();
        byte[] claveDecodificada = decoder.decode(secretKey);

        return JWT.require(Algorithm.HMAC256(claveDecodificada))
                .build()
                .verify(token)
                .getClaims();
    }

}