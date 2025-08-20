package com.hotty.user_service.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJson;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hotty.user_service.Serializers.GeoJsonPointDeserializer;
import com.hotty.user_service.validators.ValidUserData;

import lombok.Data;

@Data
@Document(collection = "users")
@JsonInclude(JsonInclude.Include.ALWAYS)
@ValidUserData // <-- Validador único a nivel de clase que lo valida todo.
public class UserDataModel {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userUID;

    private Instant lastRatingDate;

    private Integer averageReactionValue=0;

    private Integer reactionCount=0;

    private Integer totalReactionPoints=0;


    private String name;

    private String userImage1;
    private String userImage2;
    private String userImage3;
    private String userImage4;
    private String userImage5;
    private String userImage6;
    private Integer userRating;

    private String sex;
    @JsonDeserialize(using = GeoJsonPointDeserializer.class)
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private String userBio;
    private LocalDate birthDate;

    // Anidamos el objeto de configuración del usuario.
    // Se inicializa para evitar NullPointerExceptions al acceder a las
    // configuraciones de un nuevo usuario.
    private UserSettingsModel settings = new UserSettingsModel();

    // Anidamos el objeto de características del usuario.
    private UserCharacteristicsModel characteristics = new UserCharacteristicsModel();

    // Anidamos el objeto de preferencias del usuario.
    private UserCharacteristicsModel filteCharacteristicsModel = new UserCharacteristicsModel();

    // Anidamos el objeto de recompensas del usuario.
    private UserRewardsDataModel rewards = new UserRewardsDataModel();

}
