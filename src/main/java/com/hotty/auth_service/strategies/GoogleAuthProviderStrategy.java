package com.hotty.auth_service.strategies;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.hotty.auth_service.interfaces.AuthDataModelRepository;
import com.hotty.auth_service.interfaces.AuthProviderStrategy;
import com.hotty.auth_service.interfaces.AuthTokenDataModelRepository;
import com.hotty.auth_service.models.AuthDataModel;
import com.hotty.auth_service.models.AuthTokenDataModel;
import com.hotty.auth_service.services.UIDGeenrator;
import com.hotty.auth_service.services.AuthProviders.Google.GoogleTokenManager;
import com.hotty.auth_service.services.AuthTokenExpirationScheduler.TokenExpirationSchedulerService;
import com.hotty.auth_service.services.JWT.JWTService;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux; // Necesitarás Flux para getAllActiveTokensByUserUID

// Tus importaciones de servicios y repositorios
// import com.yourpackage.service.GoogleTokenManager;
// import com.yourpackage.service.JWTService;
// import com.yourpackage.service.AuthTokenModelService; // Usado en auxiliares, pero no inyectado en constructor
// import com.yourpackage.service.AuthDataModelService;   // Usado en auxiliares, pero no inyectado en constructor
// import com.yourpackage.service.TokenExpirationSchedulerService;

// Tus modelos y excepciones
// import com.yourpackage.data.AuthDataModel;
// import com.yourpackage.data.AuthTokenDataModel;
// import com.yourpackage.exceptions.CustomAuthException;
// import com.yourpackage.exceptions.NoSuchElementException; // Para getActiveTokenByUserUID o si lo usas directamente

@Component("googleAuthProviderStrategy") // Identificador del bean
public class GoogleAuthProviderStrategy implements AuthProviderStrategy {

    private final GoogleTokenManager googleTokenManager;
    private final JWTService jwtService;
    private final AuthDataModelRepository authDataModelRepository;
    private final AuthTokenDataModelRepository authTokenDataModelRepository;

    // Necesitarás inyectar estos si los usas en los auxiliares y no son
    // directamente servicios
    // private final AuthTokenModelService authTokenModelService;
    // private final AuthDataModelService authDataModelService;

    public GoogleAuthProviderStrategy(GoogleTokenManager googleTokenManager, JWTService jwtService,
            AuthDataModelRepository authDataModelRepository, AuthTokenDataModelRepository authTokenDataModelRepository,
            TokenExpirationSchedulerService tokenExpirationSchedulerService
    /*
     * , AuthTokenModelService authTokenModelService, AuthDataModelService
     * authDataModelService
     */) {
        this.googleTokenManager = googleTokenManager;
        this.jwtService = jwtService;
        this.authDataModelRepository = authDataModelRepository;
        this.authTokenDataModelRepository = authTokenDataModelRepository;
        // this.authTokenModelService = authTokenModelService;
        // this.authDataModelService = authDataModelService;
    }

    @Override
    public String getProviderId() {
        return "google";
    }

    @Override
    public Mono<AuthTokenDataModel> authenticate(String googleCode) {
        // Asumiendo que googleTokenManager.getTokenFromCode devuelve Mono<DecodedJWT>
        return googleTokenManager.getTokenFromCode(googleCode)
                .flatMap(decodedToken -> {
                    String name = decodedToken.getClaim("name").asString();
                    String email = decodedToken.getClaim("email").asString();
                    // No generes userUID aquí, AuthDataModelService debería manejarlo si es nuevo
                    // String userUID = UIDGeenrator.generateUID(10); // Generado si es nuevo

                    return authDataModelRepository.findByEmail(email)
                            .flatMap(savedAuthDataModel ->
                    // Manejar usuario existente: Invalidar tokens antiguos, generar uno nuevo
                    handleExistingUserAndInvalidateOldTokens(savedAuthDataModel, name, email))
                            // En caso de que nos devuelva NoSuchElementException, indicando que no ha
                            // encontrado
                            // lo que hemos pedido asumimos que el usuario aun no existe y por lo tanto
                            // estamos creando uno nuevo
                            .onErrorResume(error -> {

                                if (!(error instanceof NoSuchElementException)) {

                                    return Mono.error(error);

                                }

                                return handleNewUser(name, email);

                            }

                            );
                }).onErrorResume(error-> {
                    return Mono.error(error);
                });

    }

    /**
     * Maneja un usuario existente: Invalida todos sus tokens activos anteriores
     * y luego genera y guarda un nuevo token para la sesión actual.
     * 
     * @param savedAuthDataModel El modelo de datos de autenticación del usuario
     *                           existente.
     * @param name               El nombre del usuario (de Google).
     * @param email              El email del usuario (de Google).
     * @return Mono que emite el nuevo AuthTokenDataModel creado para la sesión.
     */
    private Mono<AuthTokenDataModel> handleExistingUserAndInvalidateOldTokens(AuthDataModel savedAuthDataModel,
            String name, String email) {
        // Paso 1: Encontrar e invalidar TODOS los tokens activos anteriores de este
        // usuario.
        return authTokenDataModelRepository.getAllActiveTokensByUserUID(savedAuthDataModel.getUserUID())
                .flatMap(oldActiveToken -> {

                    Duration tokenRemainingLifetime = Duration.between(Instant.now(), oldActiveToken.getExpiresAt());

                    // Revocación inmediata en Redis
                    Mono<Void> revokeInRedis = authTokenDataModelRepository
                            .revokeActiveToken(oldActiveToken.getTokenUID(), tokenRemainingLifetime);

                    // Actualización asíncrona en MongoDB para el historial
                    // authTokenDataModelRepository.updateTokenRevokedStatusInAuditLog(oldActiveToken.getTokenUID(),
                    // true);
                    // ^^^ Nota: Necesitas que updateTokenRevokedStatusInAuditLog retorne Mono<Void>
                    // o Mono<AuthTokenDataModel>
                    // para que pueda ser encadenado en un flujo reactivo si es necesario,
                    // o simplemente subscribe() si es puramente fire-and-forget.

                    // Para asegurar que la actualización de Mongo se lance, aunque sea
                    // fire-and-forget:

                    Mono<AuthTokenDataModel> updateMongo = authTokenDataModelRepository
                            .updateTokenRevokedStatusInAuditLog(oldActiveToken.getTokenUID(), true)
                            .onErrorResume(e -> {
                                System.err.println("Error updating token status in Mongo for JTI: "
                                        + oldActiveToken.getTokenUID() + " - " + e.getMessage());
                                return Mono.empty(); // No fallar el flujo principal por un fallo de auditoría asíncrono
                            });

                    return Mono.when(revokeInRedis, updateMongo); // Ejecutar ambos en paralelo y esperar que terminen
                })
                .then() // Esperar a que todos los flatMap anteriores terminen
                // Paso 2: Generar y guardar el NUEVO token para la sesión actual
                .then(jwtService.generateToken(name, email, savedAuthDataModel.getUserUID()))
                .flatMap(newAuthTokenDataModel ->{
                // Guardar el nuevo token en MongoDB para el historial (no en Redis, ya que
                // Redis es solo blacklist)

                System.out.println("Generated new token for existing user: " + savedAuthDataModel.getUserUID()
                        + " Email: " + email + " TokenUID: " + newAuthTokenDataModel.getTokenUID());
               return authTokenDataModelRepository.saveTokenToAuditLog(newAuthTokenDataModel);});
    }

    /**
     * Maneja un usuario nuevo: Crea el nuevo AuthDataModel, genera y guarda el
     * token.
     * 
     * @param name  El nombre del usuario (de Google).
     * @param email El email del usuario (de Google).
     * @return Mono que emite el nuevo AuthTokenDataModel creado para la sesión.
     */
    private Mono<AuthTokenDataModel> handleNewUser(String name, String email) {
        // Paso 1: Generar un nuevo userUID
        String userUID = UIDGeenrator.generateUID(10);

        // Paso 2: Generar el nuevo token
        return jwtService.generateToken(name, email, userUID)
                .flatMap(newAuthTokenDataModel -> {
                    // Paso 3: Crear y guardar el nuevo AuthDataModel
                    AuthDataModel newAuthDataModel = new AuthDataModel();
                    newAuthDataModel.setUserUID(userUID);
                    newAuthDataModel.setEmail(email);
                    newAuthDataModel.setAuthProvider(getProviderId());
                    // Asume que password no se establece para Google auth

                    // Guardar primero el AuthDataModel, luego el AuthTokenDataModel
                    return authDataModelRepository.save(newAuthDataModel)
                            .then(authTokenDataModelRepository.saveTokenToAuditLog(newAuthTokenDataModel));
                });
    }
}