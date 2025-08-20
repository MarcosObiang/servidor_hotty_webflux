package com.hotty.user_service.repository.interfaces;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import com.hotty.user_service.DTOs.UserDTOwithDistance;
import com.hotty.user_service.model.UserCharacteristicsModel;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserSettingsModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interfaz que define el contrato para las operaciones de persistencia
 * relacionadas con la configuración del usuario (UserSettingsModel).
 *
 * Esta abstracción permite que los casos de uso dependan de este contrato
 * en lugar de una implementación de base de datos específica.
 */
public interface UserModelRepository { // Renamed from UserUpdateRepository in previous step, now adding more methods

    /**
     * Actualiza el objeto de configuración completo para un usuario específico.
     * @param userUID El UID del usuario cuyas configuraciones se actualizarán.
     * @param settings El nuevo objeto de configuraciones a guardar.
     * @return Un Mono que emite la entidad UserDataModel completa y actualizada.
     */
    Mono<UserDataModel> updateSettings(String userUID, UserSettingsModel settings);

    /**
     * Actualiza el objeto de características completo para un usuario específico.
     * @param userUID El UID del usuario cuyas características se actualizarán.
     * @param characteristics El nuevo objeto de características a guardar.
     * @return Un Mono que emite la entidad UserDataModel completa y actualizada.
     */
    Mono<UserDataModel> updateCharacteristics(String userUID, UserCharacteristicsModel characteristics);



    /**
     * Actualiza el objeto de características para filtro completo para un usuario específico.
     * que son las que el usuario quiere que se apliquen a la hora de buscar otros usuarios.
     * @param userUID El UID del usuario cuyas características se actualizarán.
     * @param characteristics El nuevo objeto de características a guardar.
     * @return Un Mono que emite la entidad UserDataModel completa y actualizada.
     */
    Mono<UserDataModel> updateFilterCharacteristics(String userUID, UserCharacteristicsModel characteristics,Integer maxAge, Integer minAge, String preferredSex,Integer searchRadiusInKm);

    /**
     * Guarda una nueva entidad UserDataModel o actualiza una existente.
     * Si el UserDataModel tiene un ID y existe en la base de datos, se actualiza.
     * Si no tiene ID o el ID no existe, se inserta.
     *
     * @param user El objeto UserDataModel a guardar.
     * @return Un Mono que emite la entidad UserDataModel guardada.
     */
    Mono<UserDataModel> save(UserDataModel user);

    /**
     * Busca un usuario por su UserUID.
     *
     * @param userUID El UID del usuario a buscar.
     * @return Un Mono que emite el UserDataModel encontrado, o un Mono de error si no se encuentra.
     */
    Mono<UserDataModel> findByUserUID(String userUID);

    /**
     * Busca usuarios dentro de un radio geográfico específico.
     *
     * @param point El punto central de la búsqueda (longitud, latitud).
     * @param distance La distancia máxima desde el punto central.
     * @return Un Flux que emite los UserDataModel encontrados dentro del radio.
     */
    Flux<UserDTOwithDistance> findByLocationNear(GeoJsonPoint point, Distance distance, HashMap<String, Object> characteristics,Integer maxAge, Integer minAge, String preferredSex);

    /**
     * Actualiza los datos principales de un usuario.
     *
     * @param userDataModel El objeto UserDataModel con los datos a actualizar.
     * @return Un Mono que emite la entidad UserDataModel actualizada.
     */
    
    Mono<UserDataModel> updateBio(String userUID, String userBio);

    /**
     * Elimina un usuario por su UserUID.
     * @param userUID El UID del usuario a eliminar.
     * @return Un Mono<Void> que se completa cuando el usuario ha sido eliminado.
     */
    Mono<Void> deleteByUserUID(String userUID);



    Mono<UserDataModel> updateImages(String userUID, String userImage1, String userImage2, String userImage3, String userImage4, String userImage5, String userImage6);


    Mono<UserDataModel> updateLocationData(String userUID, GeoJsonPoint location);
    
    Mono<UserDataModel> updateProfileDiscoverySettings(String userUID, Map<String, Object> settings);

    Mono<UserDataModel> updateProfileAverageRating(String userUID, Integer averageRating);

    Mono<UserDataModel> updateProfileCredits(String userUID, Integer credits, Long nextDailyRewardTimestamp,Boolean waitingReward);

    Mono<UserDataModel> updateFirstRewardCredits(String userUID, Integer credits, Boolean waitingFirstReward);

    Mono<UserDataModel> substractCreditsFromUser(String userUID, Integer creditsToSpend);

    Mono<UserDataModel> addCreditsToUser(String userUID, Integer creditsToSpend);


    Mono<UserDataModel> updateDailyRewardTimestamp(String userUID, Long nextDailyRewardTimestamp);


}