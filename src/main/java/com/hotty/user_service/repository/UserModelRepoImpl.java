package com.hotty.user_service.repository;

import org.bson.Document;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import java.util.Map;
import java.util.NoSuchElementException;

import com.hotty.user_service.DTOs.UserDTOwithDistance;
import com.hotty.user_service.model.UserCharacteristicsModel;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserSettingsModel;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Repository
public class UserModelRepoImpl implements UserModelRepository {

        private final ReactiveMongoTemplate reactiveMongoTemplate;

        public UserModelRepoImpl(ReactiveMongoTemplate reactiveMongoTemplate) {
                this.reactiveMongoTemplate = reactiveMongoTemplate;
        }

        @Override
        public Mono<UserDataModel> updateSettings(String userUID, UserSettingsModel settings) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().set("settings", settings);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> updateCharacteristics(String userUID, UserCharacteristicsModel characteristics) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().set("characteristics", characteristics);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> save(UserDataModel user) {
                return reactiveMongoTemplate.save(user);
        }

        @Override
        public Mono<UserDataModel> findByUserUID(String userUID) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                return reactiveMongoTemplate.findOne(query, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Flux<UserDTOwithDistance> findByLocationNear(GeoJsonPoint point, Distance distance,
                        HashMap<String, Object> characteristics, Integer maxAge, Integer minAge, String preferredSex) {

                List<AggregationOperation> operations = new ArrayList<>();

                // $geoNear - primera etapa obligatoria para búsqueda geoespacial
                Document geoNearDoc = new Document("near",
                                new Document("type", "Point").append("coordinates",
                                                List.of(point.getX(), point.getY())))
                                .append("distanceField", "distance")
                                // CRÍTICO: Convertir la distancia a metros, ya que $geoNear lo requiere.
                                .append("maxDistance", distance.in(Metrics.KILOMETERS).getValue() * 1000)
                                .append("spherical", true);
                operations.add(context -> new Document("$geoNear", geoNearDoc));

                // $addFields para convertir birthDate a Date y calcular edad con $dateDiff
                operations.add(context -> new Document("$addFields",
                                new Document("birthDateConverted", new Document("$toDate", "$birthDate"))));
                operations.add(context -> new Document("$addFields", new Document("age",
                                new Document("$dateDiff", new Document()
                                                .append("startDate", "$birthDateConverted")
                                                .append("endDate", "$$NOW")
                                                .append("unit", "year")))));

                // Construimos $match dinámico según filtros
                List<Document> matchConditions = new ArrayList<>();

                if (characteristics != null && !characteristics.isEmpty()) {
                        characteristics.forEach((key, value) -> {
                                if (value != null && !value.toString().isEmpty()) {
                                        matchConditions.add(new Document("characteristics." + key, value));
                                }
                        });
                }

                if (minAge != null) {
                        matchConditions.add(new Document("age", new Document("$gte", minAge)));
                }
                if (maxAge != null) {
                        matchConditions.add(new Document("age", new Document("$lte", maxAge)));
                }

                if (preferredSex != null && !preferredSex.equalsIgnoreCase("Both")) {
                        matchConditions.add(new Document("sex", preferredSex));
                }

                if (!matchConditions.isEmpty()) {
                        operations.add(context -> new Document("$match", new Document("$and", matchConditions)));
                }

                // Etapa de ordenación: Se aplica después de todos los filtros para ordenar el
                // conjunto final de resultados.
                // 1. 'lastRatingDate' en orden ascendente (1): los más antiguos (valor más
                // bajo) o nulos aparecen primero.
                // 2. 'distance' en orden ascendente (1): como criterio de desempate si las
                // fechas son iguales.
                Document sortDoc = new Document("lastRatingDate", 1).append("distance", 1);
                operations.add(context -> new Document("$sort", sortDoc));

                // $project solo los campos que necesitas para el DTO
                Document projectFields = new Document()
                                .append("userUID", 1)
                                .append("name", 1)
                                .append("userImage1", 1)
                                .append("userImage2", 1)
                                .append("userImage3", 1)
                                .append("userImage4", 1)
                                .append("userImage5", 1)
                                .append("userImage6", 1)
                                .append("sex", 1)
                                .append("userBio", 1)
                                .append("birthDate", 1)
                                .append("characteristics", 1)
                                // CORRECCIÓN: Convertir la distancia de metros (devuelta por $geoNear) a
                                // kilómetros.
                                .append("distance", new Document("$divide", Arrays.asList("$distance", 1000)));

                operations.add(context -> new Document("$project", projectFields));

                Aggregation aggregation = Aggregation.newAggregation(operations);

                return reactiveMongoTemplate.aggregate(aggregation, "users", UserDTOwithDistance.class);
        }

        @Override
        public Mono<UserDataModel> updateBio(String userUID, String userBio) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().set("userBio", userBio);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<Void> deleteByUserUID(String userUID) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                return reactiveMongoTemplate.remove(query, UserDataModel.class)
                                .flatMap(result -> {
                                        if (result.getDeletedCount() == 0) {
                                                return Mono
                                                                .error(new NoSuchElementException(
                                                                                "No se encontró un usuario con el UID: "
                                                                                                + userUID));
                                        }
                                        return Mono.empty();
                                });
        }

        @Override
        public Mono<UserDataModel> updateImages(String userUID, String userImage1, String userImage2, String userImage3,
                        String userImage4, String userImage5, String userImage6) {
                Query query = new Query(Criteria.where("userUID").is(userUID));

                Update update = new Update()
                                .set("userImage1", userImage1)
                                .set("userImage2", userImage2)
                                .set("userImage3", userImage3)
                                .set("userImage4", userImage4)
                                .set("userImage5", userImage5)
                                .set("userImage6", userImage6);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> updateLocationData(String userUID, GeoJsonPoint location) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().set("location", location);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .flatMap(result -> {
                                        if (result == null) {
                                                return Mono
                                                                .error(new NoSuchElementException(
                                                                                "No se encontró un usuario con el UID: "
                                                                                                + userUID));
                                        }
                                        return Mono.just(result);
                                });
        }

        @Override
        public Mono<UserDataModel> updateProfileDiscoverySettings(String userUID, Map<String, Object> settings) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update();

                settings.forEach((key, value) -> {
                        if (value != null) {
                                update.set("settings." + key, value);
                        }
                });

                // CORRECCIÓN: Usar findAndModify para actualizar y devolver el documento en una
                // sola operación.
                // 'returnNew(true)' asegura que se devuelve el documento DESPUÉS de la
                // actualización.
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> updateFilterCharacteristics(String userUID, UserCharacteristicsModel characteristics,
                        Integer maxAge, Integer minAge, String preferredSex, Integer searchRadiusInKm) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update();
                // CORRECCIÓN: Corregido el typo de "filte" a "filter"
                update.set("filteCharacteristicsModel", characteristics);
                update.set("settings.maxAge", maxAge);
                update.set("settings.minAge", minAge);
                update.set("settings.sexPreference", preferredSex);
                update.set("settings.searchRadiusInKm", searchRadiusInKm);

                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        public Mono<UserDataModel> updateProfileAverageRating(String userUID, Integer averageRating) {

                Document query = new Document("userUID", userUID);

                Document setCalculatedValuesDoc = new Document("$set", new Document()
                                .append("totalReactionPoints", new Document("$add", Arrays.asList(
                                                new Document("$ifNull", Arrays.asList("$totalReactionPoints", 0)),
                                                averageRating)))
                                .append("reactionCount", new Document("$add", Arrays.asList( // <-- corregido aquí
                                                new Document("$ifNull", Arrays.asList("$reactionCount", 0)),
                                                1)))
                                .append("lastRatingDate", "$$NOW"));

                Document calculateAndSetAverageDoc = new Document("$set", new Document()
                                .append("averageReactionValue", new Document("$toInt",
                                                new Document("$round", new Document("$divide", Arrays.asList(
                                                                "$totalReactionPoints",
                                                                "$reactionCount"))))));

                List<Document> pipeline = List.of(setCalculatedValuesDoc, calculateAndSetAverageDoc);

                return reactiveMongoTemplate
                                .getCollection(reactiveMongoTemplate.getCollectionName(UserDataModel.class))
                                .flatMap(collection -> Mono.from(collection.findOneAndUpdate(
                                                query,
                                                pipeline,
                                                new com.mongodb.client.model.FindOneAndUpdateOptions()
                                                                .returnDocument(com.mongodb.client.model.ReturnDocument.AFTER))))
                                .map(doc -> reactiveMongoTemplate.getConverter().read(UserDataModel.class, doc))
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> updateProfileCredits(String userUID, Integer credits,
                        Long nextDailyRewardTimestamp, Boolean waitingReward) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                // CORRECCIÓN: Usar FindAndModifyOptions para devolver el documento actualizado
                // después de la actualización.
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
                Update update = new Update()
                                .set("rewards.coins", credits)
                                .set("rewards.nextDailyRewardTimestamp", nextDailyRewardTimestamp)
                                .set("rewards.waitingReward", waitingReward);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> updateFirstRewardCredits(String userUID, Integer credits,
                        Boolean waitingFirstReward) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update()
                                .set("rewards.coins", credits)
                                .set("rewards.waitingFirstReward", waitingFirstReward);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> substractCreditsFromUser(String userUID, Integer creditsToSpend) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().inc("rewards.coins", -creditsToSpend);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> updateDailyRewardTimestamp(String userUID, Long nextDailyRewardTimestamp) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().set("rewards.nextDailyRewardTimestamp", nextDailyRewardTimestamp);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> addCreditsToUser(String userUID, Integer creditsToSpend) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().inc("rewards.coins", creditsToSpend);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

        @Override
        public Mono<UserDataModel> updateDeviceNotificationToken(String userUID, String deviceNotificationToken) {
                Query query = new Query(Criteria.where("userUID").is(userUID));
                Update update = new Update().set("deviceNotificationToken", deviceNotificationToken);
                FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);

                return reactiveMongoTemplate.findAndModify(query, update, options, UserDataModel.class)
                                .switchIfEmpty(
                                                Mono.error(new NoSuchElementException(
                                                                "No se encontró un usuario con el UID: " + userUID)));
        }

}
