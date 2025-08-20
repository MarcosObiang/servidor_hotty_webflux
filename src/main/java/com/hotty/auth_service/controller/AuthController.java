package com.hotty.auth_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotty.auth_service.interfaces.AuthProviderStrategy;
import com.hotty.auth_service.interfaces.AuthProviderStrategyFactory;
import com.hotty.auth_service.models.AuthDataModel;
import com.hotty.auth_service.models.AuthTokenDataModel;
import com.hotty.ApiResponse.ApiResponse;
import com.hotty.auth_service.usecases.DeleteUserAuthDataUseCase;
import com.hotty.auth_service.usecases.GetUserAuthDataUseCase;
import com.hotty.auth_service.usecases.LogOutUseCase;
import com.hotty.auth_service.usecases.RestoreUserAuthDataUseCase;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthProviderStrategyFactory authProviderStrategyFactory;
    private final LogOutUseCase logOutUseCase;
    private final DeleteUserAuthDataUseCase deleteUserAuthDataUseCase;
    private final GetUserAuthDataUseCase getUserAuthDataUseCase;
    private final RestoreUserAuthDataUseCase restoreUserAuthDataUseCase;

    public AuthController(AuthProviderStrategyFactory authProviderStrategyFactory, LogOutUseCase logOutUseCase, 
                         DeleteUserAuthDataUseCase deleteUserAuthDataUseCase, GetUserAuthDataUseCase getUserAuthDataUseCase,
                         RestoreUserAuthDataUseCase restoreUserAuthDataUseCase) {
        this.logOutUseCase = logOutUseCase;
        this.authProviderStrategyFactory = authProviderStrategyFactory;
        this.deleteUserAuthDataUseCase = deleteUserAuthDataUseCase;
        this.getUserAuthDataUseCase = getUserAuthDataUseCase;
        this.restoreUserAuthDataUseCase = restoreUserAuthDataUseCase;
    }

    // /**
    // * Recibe el codigo de autenticacion del usuario
    // * @param code
    // * @return
    // * @throws Exception
    // */

    @PostMapping("/{provider}/callback")
    public Mono<ResponseEntity<AuthTokenDataModel>> authenticate(
            @PathVariable String provider,
            @RequestBody String authCode) { // O un DTO más específico como GoogleAuthRequest si tienes más campos

        // 1. Obtener la estrategia adecuada usando la fábrica
        AuthProviderStrategy strategy = authProviderStrategyFactory.getStrategy(provider);

        // 2. Usar la estrategia para autenticar y obtener el token.
        // El ControllerAdvice global se encargará de mapear errores a ApiResponse si es
        // necesario.
        return strategy.authenticate(authCode)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/sign-out")
    public Mono<ResponseEntity<ApiResponse<Void>>> signOut(@RequestHeader("userUID") String userUID) {
        return logOutUseCase.execute(userUID)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success("Logout successful.", null))));
        // Si logOutUseCase.execute() emite un error, se propagará.
        // Tu GlobalErrorHandlingAdvice lo interceptará y devolverá un ApiResponse de
        // error.
    }

    @GetMapping("/isValidToken")
    public Mono<ResponseEntity<ApiResponse<Boolean>>> isValidToken(@RequestHeader("Authorization") String token) {

        return Mono.just(ResponseEntity.ok(ApiResponse.success("Token validation successful.", true)));
    }


    @DeleteMapping("/delete-user-auth-data")
    public Mono<ResponseEntity<ApiResponse<Object>>> deleteUserAuthData(
            @RequestHeader("userUID") String userUID) {
        return deleteUserAuthDataUseCase.execute(userUID)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success("User authentication data deleted successfully.", null))));
    }

    @GetMapping("/get-user-auth-data")
    public Mono<ResponseEntity<ApiResponse<AuthDataModel>>> getUserAuthData(
            @RequestHeader("userUID") String userUID) {
        return getUserAuthDataUseCase.execute(userUID)
                .map(authData -> ResponseEntity.ok(ApiResponse.success("User authentication data retrieved successfully.", authData)));
    }

    @PostMapping("/restore-user-auth-data")
    public Mono<ResponseEntity<ApiResponse<AuthDataModel>>> restoreUserAuthData(
            @RequestBody AuthDataModel authDataModel) {
        return restoreUserAuthDataUseCase.execute(authDataModel)
                .map(restoredData -> ResponseEntity.ok(ApiResponse.success("User authentication data restored successfully.", restoredData)));
    }

};
