package com.hotty.auth_service.repository;

import java.time.Duration;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import com.hotty.auth_service.interfaces.AuthTokenDataModelRepository;
import com.hotty.auth_service.models.AuthTokenDataModel;

import java.util.NoSuchElementException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class AuthTokenDataModelRepositoryImpl implements AuthTokenDataModelRepository {

    ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    ReactiveMongoTemplate mongoTemplate;

    public AuthTokenDataModelRepositoryImpl(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ReactiveMongoTemplate mongoTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<AuthTokenDataModel> getAllActiveTokensByUserUID(String userUID) {

        if (userUID == null || userUID.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("El UID de usuario no puede ser nulo o vacío."));
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("userUID").is(userUID));
        query.addCriteria(Criteria.where("isRevoked").is(false)); // Asegura que solo buscamos tokens no revocados
                                                                  // explícitamente

        // Cambio: retornar Flux vacío en lugar de error cuando no hay tokens activos
        return mongoTemplate.find(query, AuthTokenDataModel.class);
        // .switchIfEmpty() eliminado - es normal no tener tokens activos
    }

    public Mono<AuthTokenDataModel> updateTokenRevokedStatusInAuditLog(String tokenUID, boolean isRevoked) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tokenUID").is(tokenUID));

        Update update = new Update().set("isRevoked", isRevoked);

        return mongoTemplate.update(AuthTokenDataModel.class)
                .matching(query)
                .apply(update)
                .withOptions(FindAndModifyOptions.options().returnNew(true)) // <--- esto es clave
                .findAndModify()
                .onErrorMap(ex -> new RuntimeException(
                        "Error al actualizar el estado de revocación del token en la auditoría para tokenUID: "
                                + tokenUID,
                        ex));
    }

    /**
     * Guarda un {@code AuthTokenDataModel} en el log de auditoría (MongoDB).
     * Este método se utiliza para mantener un registro histórico de todos los
     * tokens emitidos,
     * no para su estado activo en tiempo real.
     *
     * @param token El {@code AuthTokenDataModel} a guardar.
     * @return Un {@code Mono} que emite el {@code AuthTokenDataModel} guardado.
     * @throws IllegalArgumentException                    si el token proporcionado
     *                                                     es nulo.
     * @throws org.springframework.dao.DataAccessException si ocurre un error al
     *                                                     interactuar con MongoDB.
     */
    @Override
    public Mono<AuthTokenDataModel> saveTokenToAuditLog(AuthTokenDataModel token) {
        if (token == null) {
            return Mono.error(new IllegalArgumentException("El token a guardar no puede ser nulo."));
        }
        // mongoTemplate.save() se encarga de insertar si _id no existe o actualizar si
        // existe.
        // Asumiendo que token.getTokenUID() se mapea a _id o es el identificador único
        // que usas.
        return mongoTemplate.save(token)
                .onErrorMap(
                        e -> new RuntimeException("Error al guardar el token en el log de auditoría de MongoDB", e));
    }

    /**
     * ESTE METODO ES UN ERROR DE DISEÑO PREFIO Y SERA DEPRECATED.
     * 
     * ESTA LOGICA DEBE ESTAR EN UN USE CASE O SERVICE, NO EN EL REPOSITORIO. 
     *
     * Revoca un token activo de forma inmediata añadiendo su UID (jti) a la
     * blacklist de Redis.
     * Además, actualiza asíncronamente el estado de revocación en el log de
     * auditoría de MongoDB.
     *
     * @param tokenUID El UID (jti) del token a revocar.
     * @return Un {@code Mono<Void>} que completa cuando el token ha sido añadido a
     *         la blacklist de Redis
     *         y la actualización asíncrona a MongoDB ha sido iniciada.
     * @throws IllegalArgumentException                            si el tokenUID
     *                                                             proporcionado es
     *                                                             nulo o está
     *                                                             vacío.
     * @throws org.springframework.data.redis.RedisSystemException si ocurre un
     *                                                             error al
     *                                                             interactuar con
     *                                                             Redis.
     *                                                             (Errores de
     *                                                             MongoDB se
     *                                                             manejan
     *                                                             asíncronamente y
     *                                                             no bloquean este
     *                                                             flujo principal).
     */
    @Override
    public Mono<Void> revokeActiveToken(String tokenUID, Duration tokenRemainingLifetime) { // <-- ¡Añadir Duration!
        if (tokenUID == null || tokenUID.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("El UID del token a revocar no puede ser nulo o vacío."));
        }
        if (tokenRemainingLifetime == null || tokenRemainingLifetime.isNegative() || tokenRemainingLifetime.isZero()) {
            // Si el token ya expiró o su vida restante es cero/negativa, no es necesario
            // revocarlo en Redis
            // porque ya sería inválido por su fecha de expiración.
            // Aquí podrías decidir simplemente completar el Mono o lanzar un error
            // específico.
            // Para este ejemplo, completamos si el token ya no tiene vida útil.
            System.out.println("Advertencia: El token " + tokenUID
                    + " ya ha expirado o tiene vida útil cero/negativa. No se añadirá a la blacklist de Redis.");
            return Mono.empty(); // O Mono.error(new IllegalArgumentException("La vida útil restante del token
                                 // debe ser positiva."));
        }

        // 1. Añadir el tokenUID a la blacklist de Redis con un TTL
        // Usamos opsForValue().set() para guardar cada tokenUID como una clave
        // individual.
        // El valor puede ser simple, como "revoked" o el timestamp de revocación.
        Mono<Boolean> setRedisBlacklistEntry = reactiveRedisTemplate.opsForValue()
                .set(tokenUID, "revoked", tokenRemainingLifetime); // <-- ¡Aquí se aplica el TTL!

        // 2. Actualizar el estado de revocación en el log de auditoría de MongoDB
        // (asíncrono)
        Mono<Void> updateMongoAuditLog = mongoTemplate.updateFirst(
                Query.query(Criteria.where("tokenUID").is(tokenUID)),
                Update.update("isRevoked", true),
                AuthTokenDataModel.class)
                .doOnError(
                        e -> System.err.println("Error al actualizar el estado de revocación en MongoDB para tokenUID "
                                + tokenUID + ": " + e.getMessage()))
                .then(); // Convierte el resultado de updateFirst (UpdateResult) a Mono<Void>

        // Combinar ambas operaciones. Mono.when espera que ambas completen.
        // Retornamos Mono.when para asegurarnos de que ambas operaciones (Redis y
        // Mongo)
        // se hayan iniciado/finalizado antes de que la operación de revocación se
        // considere completa.
        return Mono.when(setRedisBlacklistEntry, updateMongoAuditLog);
    }

    @Override
    public Mono<AuthTokenDataModel> findByTokenUID(String tokenUID) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tokenUID").is(tokenUID));

        return mongoTemplate.findOne(query, AuthTokenDataModel.class)
                .switchIfEmpty(Mono.error(new NoSuchElementException(
                        "No se encontró ningún token con el tokenUID proporcionado: " + tokenUID)));
    }
}
