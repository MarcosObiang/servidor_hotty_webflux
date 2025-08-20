package com.hotty.common.common_controller;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.ApiResponse.ApiResponse;
import com.hotty.common.common_usecases.AcceptLikeUseCase;
import com.hotty.common.common_usecases.CreateLikeCommonUseCase;
import com.hotty.common.common_usecases.CreateUserUseCases;
import com.hotty.common.common_usecases.DeleteUserCommonUseCase;
import com.hotty.common.common_usecases.RevealLikeUseCase;
import com.hotty.common.common_usecases.UpdatePicturesUseCase;

import reactor.core.publisher.Mono;

@RequestMapping("/api")
@RestController
public class CommonController {

    private final CreateUserUseCases createUserUseCases;
    private final RevealLikeUseCase revealLikeUseCase;
    private final AcceptLikeUseCase acceptLikeUseCase;
    private final CreateLikeCommonUseCase createLikeUseCase;
    private final UpdatePicturesUseCase updatePicturesUseCase;
    private final DeleteUserCommonUseCase deleteUserUseCase;

    public CommonController(CreateUserUseCases createUserUseCases, RevealLikeUseCase revealLikeUseCase,
            AcceptLikeUseCase acceptLikeUseCase, CreateLikeCommonUseCase createLikeUseCase,
            UpdatePicturesUseCase updatePicturesUseCase, DeleteUserCommonUseCase deleteUserUseCase) {
        this.acceptLikeUseCase = acceptLikeUseCase;
        this.createUserUseCases = createUserUseCases;
        this.revealLikeUseCase = revealLikeUseCase;
        this.createLikeUseCase = createLikeUseCase;
        this.updatePicturesUseCase = updatePicturesUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> register(
            @RequestHeader("userUID") String userUID,

            @RequestPart("userJson") String userJson,
            @RequestPart("userImage1") FilePart userImage1,
            @RequestPart(name = "userImage2", required = false) FilePart userImage2,
            @RequestPart(name = "userImage3", required = false) FilePart userImage3,
            @RequestPart(name = "userImage4", required = false) FilePart userImage4,
            @RequestPart(name = "userImage5", required = false) FilePart userImage5,
            @RequestPart(name = "userImage6", required = false) FilePart userImage6)
            throws JsonMappingException, JsonProcessingException {
        return createUserUseCases
                .execute(userUID, userJson, userImage1, userImage2, userImage3, userImage4, userImage5, userImage6)
                .map(result -> ResponseEntity.ok(ApiResponse.success("User registered successfully", result)));
    }

    @PostMapping("/likes/{likeUID}/reveal")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> revealLike(
            @PathVariable("likeUID") String likeUID,
            @RequestHeader("userUID") String userUID) {
        // El caso de uso ahora devuelve el like actualizado.
        // El manejo de errores se delega completamente al GlobalErrorHandlingAdvice.
        return revealLikeUseCase.execute(likeUID, userUID)
                .map(result -> ResponseEntity.ok(ApiResponse.<Map<String, Object>>success("Like revealed successfully", (Map<String, Object>) result)));
    }

    @GetMapping("/accept-like")
    public Mono<ResponseEntity<ApiResponse<String>>> acceptLike(@RequestParam("likeUID") String likeUID,
            @RequestHeader("userUID") String userUID) {
        return acceptLikeUseCase.execute(likeUID, userUID)
                .map(result -> ResponseEntity.ok(ApiResponse.success("Like accepted successfully", result.toString())));
    }

    @PostMapping("/create-like")
    public Mono<ResponseEntity<ApiResponse<Object>>> createLike(@RequestHeader("userUID") String userUID,
            @RequestBody Map<String, Object> likeData) {
        return createLikeUseCase.execute(userUID, likeData)
                .map(result -> ResponseEntity.ok(ApiResponse.success("Like created successfully", result)));
        // El GlobalErrorHandlingAdvice se encargará de los errores, por lo que no es
        // necesario un .onErrorResume aquí.
    }

    @PostMapping(value = "/update-pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<Object>>> updatePictures(
            @RequestHeader("userUID") String userUID,
            @RequestPart("userImage1") FilePart userImage1,
            @RequestPart(name = "userImage2", required = false) FilePart userImage2,
            @RequestPart(name = "userImage3", required = false) FilePart userImage3,
            @RequestPart(name = "userImage4", required = false) FilePart userImage4,
            @RequestPart(name = "userImage5", required = false) FilePart userImage5,
            @RequestPart(name = "userImage6", required = false) FilePart userImage6) {
        return updatePicturesUseCase
                .execute(userUID, userImage1, userImage2, userImage3, userImage4, userImage5, userImage6)
                .map(result -> ResponseEntity.ok(ApiResponse.success("Pictures updated successfully", result)));
    }

    @DeleteMapping("/user")
    public Mono<ResponseEntity<ApiResponse<Object>>> deleteUser(@RequestHeader("userUID") String userUID) {
        return deleteUserUseCase.execute(userUID)
                .map(result -> ResponseEntity.ok(ApiResponse.success("User deleted successfully", result)))
                .onErrorResume(error -> {
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "Error deleting user: " + error.getMessage())));
                });
    }

}
