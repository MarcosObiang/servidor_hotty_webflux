package com.hotty.chat_service.interfaces;

import com.hotty.chat_service.model.ChatModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interfaz que define las operaciones de acceso a datos para la entidad ChatModel.
 * Proporciona métodos para guardar, buscar, actualizar y eliminar chats en la base de datos.
 */
public interface ChatRepository {
    /**
     * Guarda un nuevo chat en la base de datos.
     *
     * @param chat El objeto ChatModel a guardar.
     * @return Un Mono que emite el ChatModel guardado.
     */
    Mono<ChatModel> save(ChatModel chat);

    /**
     * Busca un chat por su ID único.
     *
     * @param chatId El ID del chat a buscar.
     * @return Un Mono que emite el ChatModel encontrado, o un Mono vacío si no se encuentra.
     */
    Mono<ChatModel> findById(String chatId);

    /**
     * Busca todos los chats en los que un usuario está involucrado, ya sea como user1 o user2.
     *
     * @param userId El ID del usuario para filtrar los chats.
     * @return Un Flux que emite los ChatModel encontrados.
     */
    Flux<ChatModel> findByUserId(String userId);

    /**
     * Elimina un chat por su ID.
     *
     * @param chatId El ID del chat a eliminar.
     * @return Un Mono vacío que se completa cuando el chat se elimina correctamente.
     */
    Mono<ChatModel> deleteById(String chatId);

    /**
     * Actualiza un chat existente.
     *
     * @param chat El objeto ChatModel con los datos actualizados.
     * @return Un Mono que emite el ChatModel actualizado.
     */
    Mono<ChatModel> update(ChatModel chat);

    /**
     * Busca un chat entre dos usuarios específicos, independientemente del orden en que aparecen en el chat.
     *
     * @param userId1 El ID del primer usuario.
     * @param userId2 El ID del segundo usuario.
     * @return Un Mono que emite el ChatModel encontrado, o un Mono vacío si no existe un chat entre esos usuarios.
     */
    Mono<ChatModel> findByUserPair(String userId1, String userId2);

    /**
     * Elimina todos los chats en los que un usuario está involucrado, ya sea como user1 o user2.
     *
     * @param userId El ID del usuario para filtrar los chats a eliminar.
     * @return Un Flux que emite los chats eliminados.
     */
    Flux<ChatModel> deleteAllChatsByUserUID(String userId);
}
