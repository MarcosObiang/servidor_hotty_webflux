package com.hotty.chat_service.repo;

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;

import com.hotty.chat_service.model.MessageModel;
import com.mongodb.client.result.DeleteResult;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Repositorio personalizado para la entidad {@link MessageModel} que utiliza
 * {@link ReactiveMongoTemplate} directamente para las operaciones de base de datos.
 * <p>
 * Esta clase proporciona un control más granular sobre las interacciones con MongoDB
 * en comparación con el uso de interfaces {@code ReactiveMongoRepository}.
 * Es responsable de implementar explícitamente las operaciones CRUD.
 * </p>
 * <p>
 * Las operaciones devuelven publicadores reactivos ({@link Mono} para un solo elemento o vacío,
 * {@link Flux} para múltiples elementos). Las excepciones que ocurren durante las operaciones
 * de base de datos se propagan como señales de error en estos publicadores, y pueden ser
 * manejadas usando operadores reactivos como {@code onErrorResume}, {@code onErrorMap}, etc.,
 * en la capa de servicio que consume este repositorio.
 * </p>
 * <p>
 * El tipo de ID para {@link MessageModel} se asume que es {@link String}.
 * </p>
 */
@Repository
public class MessageModelRepo {

    private static final Logger log = LoggerFactory.getLogger(MessageModelRepo.class);

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    /**
     * Constructor para la inyección de {@link ReactiveMongoTemplate}.
     *
     * @param reactiveMongoTemplate El template para interactuar con MongoDB de forma reactiva.
     */
    public MessageModelRepo(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    /**
     * Guarda (inserta o actualiza) una entidad {@link MessageModel}.
     * Si la entidad tiene un ID y existe en la base de datos, se actualiza.
     * Si no tiene ID o el ID no existe, se inserta.
     *
     * @param message La entidad {@link MessageModel} a guardar.
     * @return Un {@link Mono} que emite la entidad guardada.
     */
    public Mono<MessageModel> save(MessageModel message) {
        if (message == null) {
            return Mono.error(new IllegalArgumentException("El objeto MessageModel a guardar no puede ser nulo."));
        }
        return reactiveMongoTemplate.save(message)
            .doOnError(error -> log.error("Error al guardar MessageModel con messageId '{}': {}", message.getMessageId(), error.getMessage(), error))
            .onErrorMap(DataAccessException.class, e -> new RuntimeException("Error de base de datos al guardar el mensaje con messageId: " + message.getMessageId(), e));
    }

    /**
     * Busca todos los mensajes donde el userUID proporcionado es el emisor o el receptor.
     *
     * @param userUID El UID del usuario para buscar en los campos senderId o recieverId.
     * @return Un {@link Flux} que emite todos los mensajes encontrados que coinciden con el criterio.
     */
    public Flux<MessageModel> findBySenderIdOrRecieverId(String userUID) {
        if (userUID == null || userUID.isBlank()) {
            return Flux.error(new IllegalArgumentException("El userUID no puede ser nulo o estar vacío."));
        }
        // Crea un criterio donde el campo 'senderId' es userUID O 'recieverId' es userUID.
        Criteria senderCriteria = Criteria.where("senderId").is(userUID);
        Criteria receiverCriteria = Criteria.where("recieverId").is(userUID);
        Query query = Query.query(new Criteria().orOperator(senderCriteria, receiverCriteria));

        return reactiveMongoTemplate.find(query, MessageModel.class)
            .doOnError(error -> log.error("Error al buscar mensajes por userUID '{}': {}", userUID, error.getMessage(), error))
            .onErrorMap(DataAccessException.class, e -> new RuntimeException("Error de base de datos al buscar mensajes para el usuario: " + userUID, e));
    }
    /**
     * Busca una entidad {@link MessageModel} por su ID.
     *
     * @param id El ID de la entidad a buscar (se asume que es un {@link String}).
     * @return Un {@link Mono} que emite la entidad encontrada, o un {@link Mono} vacío si no se encuentra.
     */
    public Mono<MessageModel> findById(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return Mono.error(new IllegalArgumentException("El messageId no puede ser nulo o estar vacío."));
        }
        
        // CORRECCIÓN: Buscar por el campo messageId, no por el id de MongoDB
        Query query = Query.query(Criteria.where("messageId").is(messageId));
        return reactiveMongoTemplate.findOne(query, MessageModel.class)
            .switchIfEmpty(Mono.error(new NoSuchElementException("No se encontró ningún mensaje con el messageId: " + messageId)))
            .onErrorMap(e -> !(e instanceof NoSuchElementException), e -> new RuntimeException("Error de base de datos al buscar mensaje por messageId: " + messageId, e));
    }

    /**
     * Devuelve todas las entidades {@link MessageModel} de la colección.
     *
     * @return Un {@link Flux} que emite todas las entidades {@link MessageModel}.
     */
    public Flux<MessageModel> findAll() {
        return reactiveMongoTemplate.findAll(MessageModel.class)
            .doOnError(error -> log.error("Error al buscar todos los MessageModel: {}", error.getMessage(), error))
            .onErrorMap(DataAccessException.class, e -> new RuntimeException("Error de base de datos al buscar todos los mensajes.", e));
    }

    /**
     * CORREGIDO: Elimina por messageId en lugar de id
     */
    public Mono<Void> deleteById(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return Mono.error(new IllegalArgumentException("El messageId a eliminar no puede ser nulo o estar vacío."));
        }
        
        // CORRECCIÓN: Eliminar por messageId, no por id de MongoDB
        Query query = Query.query(Criteria.where("messageId").is(messageId));
        return reactiveMongoTemplate.remove(query, MessageModel.class)
            .flatMap(deleteResult -> {
                if (deleteResult.getDeletedCount() == 0) {
                    return Mono.error(new NoSuchElementException("No se encontró ningún mensaje para eliminar con el messageId: " + messageId));
                }
                return Mono.empty();
            })
            .onErrorMap(e -> !(e instanceof NoSuchElementException), e -> new RuntimeException("Error de base de datos al eliminar mensaje por messageId: " + messageId, e))
            .then();
    }

    public Mono<Void> deleteAll() {
        // Crea una consulta vacía para que coincida con todos los documentos de la colección.
        Query query = new Query();
        return reactiveMongoTemplate.remove(query, MessageModel.class) // Esto elimina todos los documentos de la colección
            .onErrorMap(DataAccessException.class, e -> new RuntimeException("Error de base de datos al intentar eliminar todos los mensajes.", e))
            .then();
    }

    /**
     * Busca todos los mensajes que pertenecen a un UID de chat específico.
     *
     * @param chatUID El UID del chat por el cual filtrar los mensajes.
     * @return Un {@link Flux} que emite todos los mensajes encontrados para el chat dado.
     */
    public Flux<MessageModel> findByChatUID(String chatUID) {
        if (chatUID == null || chatUID.isBlank()) {
            return Flux.error(new IllegalArgumentException("El chatUID no puede ser nulo o estar vacío."));
        }
        Query query = Query.query(Criteria.where("chatUID").is(chatUID));
        return reactiveMongoTemplate.find(query, MessageModel.class)
            .onErrorMap(DataAccessException.class, e -> new RuntimeException("Error de base de datos al buscar mensajes por chatUID: " + chatUID, e));
    }

    /**
     * CORREGIDO: Lógica simplificada y más robusta para marcar como leído
     */
    public Flux<MessageModel> markMessagesAsReadByIds(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Flux.error(new IllegalArgumentException("La lista de messageIds no puede ser nula o estar vacía."));
        }

        // 1. Actualizar directamente en la base de datos
        Query updateQuery = Query.query(
            Criteria.where("messageId").in(messageIds)
            .and("readByReciever").is(false)  // Solo actualizar los que no están leídos
        );
        Update update = new Update().set("readByReciever", true);
        
        return reactiveMongoTemplate.updateMulti(updateQuery, update, MessageModel.class)
            .flatMapMany(updateResult -> {
                log.info("Mensajes actualizados: {}", updateResult.getModifiedCount());
                
                // 2. Recuperar todos los mensajes solicitados (actualizados o no)
                Query findQuery = Query.query(Criteria.where("messageId").in(messageIds));
                return reactiveMongoTemplate.find(findQuery, MessageModel.class);
            })
            .onErrorMap(DataAccessException.class, e -> new RuntimeException("Error de base de datos al marcar mensajes como leídos.", e));
    }

    /**
     * Elimina todos los mensajes que pertenecen a un UID de chat específico.
     *
     * @param chatUID El UID del chat cuyos mensajes se eliminarán.
     * @return Un {@link Mono} que emite el número de documentos eliminados.
     */
    public Mono<Long> deleteByChatUID(String chatUID) {
        if (chatUID == null || chatUID.isBlank()) {
            return Mono.error(new IllegalArgumentException("El chatUID no puede ser nulo o estar vacío."));
        }
        Query query = Query.query(Criteria.where("chatUID").is(chatUID));
        return reactiveMongoTemplate.remove(query, MessageModel.class)
            .map(DeleteResult::getDeletedCount)
            .onErrorMap(DataAccessException.class, e -> new RuntimeException("Error de base de datos al eliminar mensajes por chatUID: " + chatUID, e));
    }
}
