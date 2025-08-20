package com.hotty.chat_service.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.hotty.chat_service.interfaces.ChatRepository;
import com.hotty.chat_service.model.ChatModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.NoSuchElementException;

/**
 * Implementación del repositorio para gestionar operaciones CRUD reactivas
 * sobre documentos ChatModel en MongoDB utilizando ReactiveMongoTemplate.
 */
@Repository
public class ChatRepositoryImpl implements ChatRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    /**
     * Constructor con inyección del ReactiveMongoTemplate.
     *
     * @param mongoTemplate la plantilla reactiva para operaciones MongoDB
     */
    public ChatRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Guarda un nuevo chat en la base de datos.
     * Si el chat con el mismo chatId ya existe, se emitirá un error de clave
     * duplicada.
     *
     * @param chat objeto ChatModel a guardar
     * @return Mono con el chat guardado o error si ya existe o falla la operación
     */
    @Override
    public Mono<ChatModel> save(ChatModel chat) {
        return mongoTemplate.insert(chat)
                // Manejo específico para error de clave duplicada, preservando la causa.
                .onErrorResume(DuplicateKeyException.class, e -> 
                        Mono.error(new IllegalStateException("Chat ID duplicado: " + chat.getChatId(), e)))
                // Manejo genérico para otros errores, asegurando no re-envolver la excepción anterior.
                .onErrorMap(e -> !(e instanceof IllegalStateException), e -> 
                        new RuntimeException("Error al guardar el chat: " + e.getMessage(), e));
    }

    /**
     * Busca un chat por su ID único.
     *
     * @param chatId ID del chat a buscar
     * @return Mono con el chat encontrado o error si no existe
     */
    @Override
    public Mono<ChatModel> findById(String chatId) {
        return mongoTemplate.findById(chatId, ChatModel.class)
                // Mapear errores de base de datos a una excepción de runtime, preservando la causa.
                .onErrorMap(e -> new RuntimeException("Error en BD al buscar chat por ID: " + e.getMessage(), e))
                // Emitir un error semánticamente correcto si no se encuentra el chat.
                .switchIfEmpty(Mono.error(new NoSuchElementException("Chat no encontrado con ID: " + chatId)));
    }

    /**
     * Obtiene todos los chats donde el usuario está involucrado (como user1 o
     * user2).
     *
     * @param userId ID del usuario para filtrar chats
     * @return Flux con todos los chats que involucran al usuario o error en la
     *         consulta
     */
    @Override
    public Flux<ChatModel> findByUserId(String userId) {
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("user1Id").is(userId),
                Criteria.where("user2Id").is(userId));
        Query query = new Query(criteria);

        // Mapear cualquier error de base de datos, preservando la causa original.
        return mongoTemplate.find(query, ChatModel.class)
                .onErrorMap(e -> new RuntimeException("Error al buscar chats del usuario: " + e.getMessage(), e));
    }

    /**
     * Elimina un chat dado su ID.
     *
     * @param chatId ID del chat a eliminar
     * @return Mono con el chat eliminado, o error si no se encuentra o falla la operación
     */
    @Override
    public Mono<ChatModel> deleteById(String chatId) {
        Query query = new Query(Criteria.where("chatId").is(chatId));
        // Usamos findAndRemove que atómicamente encuentra y elimina un documento, devolviéndolo.
        return mongoTemplate.findAndRemove(query, ChatModel.class)
                // Mapear errores de base de datos a una excepción de runtime, preservando la causa.
                .onErrorMap(e -> new RuntimeException("Error en BD al eliminar chat: " + e.getMessage(), e))
                // Si no se encuentra ningún documento para eliminar, findAndRemove devuelve un Mono vacío.
                // Usamos switchIfEmpty para convertir ese caso en un error semánticamente correcto.
                .switchIfEmpty(Mono.error(new NoSuchElementException("No se encontró el chat a eliminar con ID: " + chatId)));
    }

    /**
     * Actualiza un chat existente o crea uno nuevo si no existe,
     * utilizando la operación save que sobrescribe el documento por ID.
     *
     * @param chat objeto ChatModel a actualizar
     * @return Mono con el chat actualizado o error si falla la operación
     */
    @Override
    public Mono<ChatModel> update(ChatModel chat) {
        return mongoTemplate.save(chat)
                .onErrorMap(e -> new RuntimeException("Error al actualizar chat: " + e.getMessage(), e));
    }

    /**
     * Busca un chat entre dos usuarios independientemente del orden.
     */
    public Mono<ChatModel> findByUserPair(String userId1, String userId2) {
        Criteria criteria = new Criteria().orOperator(
                new Criteria().andOperator(
                        Criteria.where("user1Id").is(userId1),
                        Criteria.where("user2Id").is(userId2)),
                new Criteria().andOperator(
                        Criteria.where("user1Id").is(userId2),
                        Criteria.where("user2Id").is(userId1)));
        Query query = new Query(criteria);

        // Mapear cualquier error de base de datos, preservando la causa original.
        return mongoTemplate.findOne(query, ChatModel.class)
                .onErrorMap(e -> new RuntimeException("Error al buscar chat por pareja de usuarios: " + e.getMessage(), e))
                // Emitir un error semánticamente correcto si no se encuentra el chat.
                .switchIfEmpty(Mono.error(new NoSuchElementException("No se encontró un chat para la pareja de usuarios: " + userId1 + ", " + userId2)));
    }

    /**
     * Elimina todos los chats donde el usuario está involucrado (como user1 o
     * user2).
     *
     * @param userId ID del usuario para filtrar chats
     * @return Flux que emite los chats eliminados. Completa vacío si no se encuentra ninguno.
     */

    @Override
    public Flux<ChatModel> deleteAllChatsByUserUID(String userId) {
        // Consulta para encontrar todos los chats del usuario.
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("user1Id").is(userId),
                Criteria.where("user2Id").is(userId));
        Query query = new Query(criteria);

        // 1. Encontrar todos los chats que coinciden.
        return mongoTemplate.find(query, ChatModel.class)
                .collectList() // 2. Recolectarlos en una lista.
                .flatMapMany(chatsToDelete -> {
                    if (chatsToDelete.isEmpty()) {
                        // Si no hay chats, devolver un Flux vacío.
                        return Flux.empty();
                    }
                    // 3. Crear una consulta para eliminar esos chats por sus IDs.
                    java.util.List<String> chatIds = chatsToDelete.stream().map(ChatModel::getChatId).toList();
                    Query deleteQuery = new Query(Criteria.where("chatId").in(chatIds));
                    
                    // 4. Ejecutar la eliminación y luego emitir los chats que se eliminaron.
                    return mongoTemplate.remove(deleteQuery, ChatModel.class)
                                        .thenMany(Flux.fromIterable(chatsToDelete));
                })
                .onErrorMap(e -> new RuntimeException("Error en BD al eliminar chats del usuario: " + e.getMessage(), e));
    }
}
