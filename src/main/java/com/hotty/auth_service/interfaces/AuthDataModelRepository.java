package com.hotty.auth_service.interfaces;

import com.hotty.auth_service.models.AuthDataModel;

import reactor.core.publisher.Mono;

public interface AuthDataModelRepository {

    /**
     * Busca un AuthDataModel por su userUID.
     * 
     * @param userUID El UID del usuario.
     * @return Un Mono que emite el AuthDataModel si se encuentra, o vacío si no.
     */
    Mono<AuthDataModel> findUserByUID(String userUID);

    /**
     * Busca un AuthDataModel por su dirección de correo electrónico.
     * 
     * @param email El correo electrónico del usuario.
     * @return Un Mono que emite el AuthDataModel si se encuentra, o vacío si no.
     */
    Mono<AuthDataModel> findByEmail(String email);

    /**
     * Guarda (crea o actualiza) un AuthDataModel.
     * 
     * @param authData El AuthDataModel a guardar.
     * @return Un Mono que emite el AuthDataModel guardado.
     */
    Mono<AuthDataModel> save(AuthDataModel authData);

    /**
     * Elimina un AuthDataModel por su ID de documento.
     * 
     * @param id El ID del documento a eliminar.
     * @return Un Mono que completa cuando la operación ha terminado.
     */
    Mono<Void> deleteByUserUID(String id);
}
