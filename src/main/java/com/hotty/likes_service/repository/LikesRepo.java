package com.hotty.likes_service.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.hotty.likes_service.model.LikeModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

/**
 * Repositorio personalizado para manejar operaciones reactivas sobre la
 * colección de "likes".
 * Utiliza ReactiveMongoTemplate para mayor flexibilidad sobre las operaciones
 * en MongoDB.
 */

@Repository
public class LikesRepo {

    private final ReactiveMongoTemplate template;

    /**
     * Constructor para inyectar el template reactivo de Mongo.
     *
     * @param template ReactiveMongoTemplate
     */
    public LikesRepo(ReactiveMongoTemplate template) {
        this.template = template;
    }

    /**
     * Obtiene todos los likes recibidos por un usuario específico.
     *
     * @param userUID ID del usuario que recibe los likes.
     * @return Un {@link Flux} con todos los {@link LikeModel} encontrados.
     * @throws IllegalArgumentException si userUID es nulo o está vacío.
     */
    public Flux<LikeModel> getAll(String userUID) {
        if (userUID == null || userUID.isBlank()) {
            return Flux.error(new IllegalArgumentException("El User UID no puede ser nulo o vacío."));
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverUID").is(userUID));

        return template.find(query, LikeModel.class)
                .onErrorResume(error -> {
                    // En una aplicación real, usar un logger estructurado.
                    System.err.println("Error al obtener likes para el usuario " + userUID + ": " + error.getMessage());
                    return Flux.error(new RuntimeException("Error en la base de datos al obtener likes.", error));
                });
    }

    /**
     * Guarda un nuevo like en la base de datos.
     *
     * @param like El objeto {@link LikeModel} a guardar.
     * @return Un {@link Mono} con el like guardado.
     * @throws IllegalArgumentException si el objeto like es nulo.
     * @throws IllegalStateException    si ya existe un like con el mismo likeUID.
     */
    public Mono<LikeModel> add(LikeModel like) {
        if (like == null) {
            return Mono.error(new IllegalArgumentException("El objeto LikeModel no puede ser nulo."));
        }
        return template.save(like)
                .onErrorResume(DuplicateKeyException.class, e -> Mono
                        .error(new IllegalStateException("Ya existe un like con el UID: " + like.getLikeUID(), e)))
                .onErrorResume(error -> Mono
                        .error(new RuntimeException("Error al guardar el like: " + error.getMessage(), error)));
    }

    /**
     * Elimina un like por su identificador único.
     *
     * @param likeUID El ID único del like.
     * @return Un {@link Mono<Void>} que completa si se elimina correctamente.
     * @throws IllegalArgumentException si likeUID es nulo o está vacío.
     * @throws NoSuchElementException   si no se encuentra ningún like con ese UID
     *                                  para eliminar.
     */
    public Mono<Object> delete(String likeUID) {
        if (likeUID == null || likeUID.isBlank()) {
            return Mono.error(new IllegalArgumentException("El Like UID no puede ser nulo o vacío."));
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("likeUID").is(likeUID));

        return template.remove(query, LikeModel.class)
                .flatMap(result -> {
                    if (result.getDeletedCount() == 0) {
                        return Mono.error(
                                new NoSuchElementException(
                                        "No se encontró ningún like para eliminar con el UID: " + likeUID));
                    }
                    return Mono.empty(); // Éxito, completa el Mono<Void>
                })
                .onErrorResume(NoSuchElementException.class, Mono::error) // Re-lanza la excepción específica
                .onErrorResume(error -> Mono
                        .error(new RuntimeException("Error en BD al eliminar el like: " + error.getMessage(), error)));
    }

    /**
     * Elimina todos los likes donde el usuario es el emisor o el receptor.
     *
     * @param userUID El ID del usuario.
     * @return Un {@link Mono} que emite el número de likes eliminados. No emite
     *         error si no se encuentra ninguno.
     * @throws IllegalArgumentException si userUID es nulo o está vacío.
     */
    public Mono<Long> deleteAll(String userUID) {
        if (userUID == null || userUID.isBlank()) {
            return Mono.error(new IllegalArgumentException("El User UID no puede ser nulo o vacío."));
        }
        Query query = new Query();
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("receiverUID").is(userUID),
                Criteria.where("senderUID").is(userUID)));

        return template.remove(query, LikeModel.class)
                .map(result -> result.getDeletedCount())
                .onErrorResume(error -> Mono.error(new RuntimeException(
                        "Error en BD al eliminar los likes del usuario: " + error.getMessage(), error)));
    }

    /**
     * Busca un like por su UID.
     *
     * @param likeUID El ID único del like.
     * @return Un {@link Mono} con el like encontrado.
     * @throws IllegalArgumentException si likeUID es nulo o está vacío.
     * @throws NoSuchElementException   si no se encuentra el like.
     */
    public Mono<LikeModel> findByLikeUID(String likeUID) {
        if (likeUID == null || likeUID.isBlank()) {
            return Mono.error(new IllegalArgumentException("El Like UID no puede ser nulo o vacío."));
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("likeUID").is(likeUID));

        return template.findOne(query, LikeModel.class)
                .switchIfEmpty(Mono.error(new NoSuchElementException("No se encontró el like con el UID: " + likeUID)))
                .onErrorResume(ex -> {
                    if (ex instanceof NoSuchElementException) {
                        return Mono.error(ex); // Re-lanza la excepción específica
                    }
                    return Mono.error(new RuntimeException("Error en BD al buscar el like: " + ex.getMessage(), ex));
                });
    }

}
