package com.hotty.gateway_server.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.hotty.gateway_server.DTOs.AuthDTO;
import com.hotty.gateway_server.feignInterfaces.AuthServiceGoogleInterface;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/oauth")
public class OAuth2LoginController {

    private final WebClient.Builder webClientBuilder;

    public OAuth2LoginController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    // @GetMapping("/google/code")
    // public Mono<ResponseEntity<String>> googleCallback(@RequestParam("code") String code) {
    //     System.out.println(code);
    //     return webClientBuilder.build()
    //             .post()
    //             .uri("http://auth-service/internal/google/oauth2callback")
    //             .bodyValue(code)
    //             .retrieve()
    //             .toEntity(AuthDTO.class)
    //             .flatMap(response -> {
    //                 // Verifica que el cuerpo de la respuesta no sea nulo
    //                 String token = response.getHeaders().getFirst("token");
    //                 String userUID = response.getHeaders().getFirst("userUID");
    //                 String expiresAt = (response.getHeaders().getFirst("expiresAt"));
    //                 if (token == null || token.isEmpty()) {
    //                     return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                             .body("El token no fue recibido correctamente del MS Auth"));
    //                 }

    //                                 // Verifica que userUID y expiresAt no sean nulos
    //             if (userUID == null || expiresAt == null) {
    //                 return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                         .body("Los encabezados userUID o expiresAt no fueron recibidos correctamente"));
    //             }

    //             AuthDTO authDTO = new AuthDTO(token, userUID, expiresAt);
                

    //                 // Construye la URL de redirección con el token y un timestamp
    //                 String redirectUrl = "myapp://auth-success?token=" + token + "&timestamp="
    //                         + Instant.now().getEpochSecond() + "&userUID=" + userUID + "&expiresAt=" + expiresAt;

    //                 // Configura los encabezados de redirección
    //                 HttpHeaders httpHeaders = new HttpHeaders();
    //                 httpHeaders.setLocation(URI.create(redirectUrl));
    //                 httpHeaders.add("Cache-Control", "no-cache, no-store, must-revalidate");
    //                 httpHeaders.add("Pragma", "no-cache");
    //                 httpHeaders.add("Expires", "0");


                    

    //                 // Retorna la redirección como respuesta
    //                 return Mono.just(
    //                         ResponseEntity.status(HttpStatus.FOUND)
    //                                 .headers(httpHeaders)
    //                                 .body(authDTO.toString()));
    //             });
    // }

}
