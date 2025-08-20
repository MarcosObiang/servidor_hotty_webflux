package com.hotty.likes_service.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotty.ApiResponse.ApiResponse;
import com.hotty.likes_service.DTOs.UpdateLikeRequest;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.likes_service.usecases.CreateLikeUseCase;
import com.hotty.likes_service.usecases.DeleteAllLikesBYUserUseCase;
import com.hotty.likes_service.usecases.DeleteLikeUseCase;
import com.hotty.likes_service.usecases.GetLikeUseCase;
import com.hotty.likes_service.usecases.GetallLikesByUserUIDUseCase;
import com.hotty.likes_service.usecases.UpdateLikeUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/likes")
public class LikesController {

    private final CreateLikeUseCase createLikeUseCase;
    private final GetallLikesByUserUIDUseCase getAllLikesByUserUIDUseCase;
    private final DeleteLikeUseCase deleteLikeUseCase;
    private final DeleteAllLikesBYUserUseCase deleteAllLikesBYUserUseCase;
    private final UpdateLikeUseCase updateLikeUseCase;
    private final GetLikeUseCase getLikeUseCase;

    public LikesController(CreateLikeUseCase createLikeUseCase, GetallLikesByUserUIDUseCase getAllLikesByUserUIDUseCase,
            DeleteLikeUseCase deleteLikeUseCase, DeleteAllLikesBYUserUseCase deleteAllLikesBYUserUseCase,
            UpdateLikeUseCase updateLikeUseCase, GetLikeUseCase getLikeUseCase) {
        this.createLikeUseCase = createLikeUseCase;
        this.getAllLikesByUserUIDUseCase = getAllLikesByUserUIDUseCase;
        this.deleteLikeUseCase = deleteLikeUseCase;
        this.deleteAllLikesBYUserUseCase = deleteAllLikesBYUserUseCase;
        this.updateLikeUseCase = updateLikeUseCase;
        this.getLikeUseCase = getLikeUseCase;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<LikeModel>>>> getLikes(@RequestHeader("userUID") String userUID) {
        return getAllLikesByUserUIDUseCase.execute(userUID)
                .collectList()
                .map(likes -> ResponseEntity.ok(ApiResponse.success("Likes obtenidos exitosamente", likes)));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<LikeModel>>> addLike(
            @RequestHeader("userUID") String senderUID,
            @RequestParam("receiverUID") String receiverUID,
            @RequestParam("likeValue") Integer likeValue) {
        // El parámetro likedPetUID ya no es necesario según el caso de uso
        // refactorizado
        return createLikeUseCase.execute(senderUID, receiverUID, likeValue)
                .map(like -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Like creado exitosamente", like)));
    }

    @DeleteMapping("/{likeUID}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteLike(
            @PathVariable("likeUID") String likeUID,
            @RequestHeader("userUID") String userUID) {
        // El caso de uso ahora devuelve Mono<Void>, por lo que usamos .then()
        // para transformar el éxito en una respuesta 204 No Content.
        return deleteLikeUseCase.execute(userUID, likeUID)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success("Like eliminado exitosamente", null))));
    }

    @DeleteMapping("/deleteAll")
    public Mono<ResponseEntity<ApiResponse<Long>>> deleteAllLikes(@RequestHeader("userUID") String userUID) {
        // El caso de uso ahora devuelve Mono<Long> con el conteo de likes eliminados.
        return deleteAllLikesBYUserUseCase.execute(userUID)
                .map(count -> ResponseEntity.ok(ApiResponse.success("Todos los likes eliminados exitosamente", count)));
    }

    /**
     * 
     * TODO: Añadir lógica de autorización en el caso de uso para asegurar que el
     * `userUID` del header
     * tiene permiso para ver este `like` (ej. si es el emisor o el receptor).
     */
    @GetMapping("/{likeUID}")
    public Mono<ResponseEntity<ApiResponse<LikeModel>>> getLike(
            @PathVariable("likeUID") String likeUID,
            @RequestHeader("userUID") String userUID) {
        return getLikeUseCase.execute(likeUID)
                .map(like -> ResponseEntity.ok(ApiResponse.success("Like obtenido exitosamente", like)));
    }

    /**
     * Actualiza un like. Solo el receptor del like puede modificarlo.
     * Actualmente, solo se puede modificar el estado 'isRevealed'.
     *
     * @param likeUID El UID del like a actualizar.
     * @param userUID El UID del usuario que realiza la acción (inyectado desde el
     *                header).
     * @param request El cuerpo de la petición con los campos a actualizar.
     * @return Un Mono con el LikeModel actualizado y estado 200 OK.
     */
    @PutMapping("/{likeUID}")
    public Mono<ResponseEntity<ApiResponse<LikeModel>>> updateLike(
            @PathVariable("likeUID") String likeUID,
            @RequestHeader("userUID") String userUID,
            @RequestParam("senderPictureUrl") String senderPictureUrl,
            @RequestParam("senderBirthDate") String senderBirthDate,
            @RequestParam("senderName") String senderName) {
        return updateLikeUseCase.execute(likeUID, userUID, true, senderPictureUrl,
                senderBirthDate != null ? LocalDate.parse(senderBirthDate) : null, senderName)
                .map(like -> ResponseEntity.ok(ApiResponse.success("Like actualizado exitosamente", like)));
    }

}
