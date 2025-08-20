package com.hotty.user_service.controller;

import java.util.Map;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotty.user_service.DTOs.UserDTO;
import com.hotty.user_service.DTOs.UpdateFilterCharacteristicsRequest;
import com.hotty.user_service.DTOs.UserDTOwithDistance;
import com.hotty.user_service.Serializers.GeoJsonPointDeserializer;
import com.hotty.user_service.model.UserCharacteristicsModel;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserSettingsModel;
import com.hotty.ApiResponse.ApiResponse;
import com.hotty.user_service.usecases.ClaimFirstRewardUseCase;
import com.hotty.user_service.usecases.CreateUserUseCase;
import com.hotty.user_service.usecases.DeleteUserUseCase;
import com.hotty.user_service.usecases.GetUserByPositionUseCase;
import com.hotty.user_service.usecases.GetUserByUIDUseCase;
import com.hotty.user_service.usecases.MakePurchaseForUserWithCreditsUseCase;
import com.hotty.user_service.usecases.RenewUserCreditsUseCase;
import com.hotty.user_service.usecases.UpdateAverageRatingUseCase;
import com.hotty.user_service.usecases.UpdateBioUseCase;
import com.hotty.user_service.usecases.UpdateFilterCharacteristicsUseCase;
import com.hotty.user_service.usecases.UpdateProfileDiscoverySettingsUseCase;
import com.hotty.user_service.usecases.UpdateUserCharacteristicsUseCase;
import com.hotty.user_service.usecases.UpdateUserImagesUseCase;
import com.hotty.user_service.usecases.UpdateUserLocationUseCase;
import com.hotty.user_service.usecases.UpdateUserSettingsUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List; // Import for List
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/users")
public class Controller {

        private final CreateUserUseCase createUserUseCase;
        private final GetUserByUIDUseCase getUserByUIDUseCase;
        private final GetUserByPositionUseCase getUserByPositionUseCase;
        private final UpdateUserImagesUseCase updateUserImagesUseCase;
        private final UpdateUserCharacteristicsUseCase updateUserCharacteristicsUseCase;
        private final UpdateUserSettingsUseCase updateUserSettingsUseCase;
        private final UpdateUserLocationUseCase updateUserLocationUseCase;
        private final DeleteUserUseCase deleteUserUseCase;
        private final UpdateBioUseCase updateBioUseCase;
        private final UpdateProfileDiscoverySettingsUseCase updateProfilediscoverySettings;
        private final UpdateFilterCharacteristicsUseCase updateFilterCharacteristicsUseCase;
        private final UpdateAverageRatingUseCase updateAverageRatingUseCase; // Use case for updating average rating
        private final RenewUserCreditsUseCase renewUserCreditsUseCase; // Use case for renewing user credits
        private final ClaimFirstRewardUseCase claimFirstRewardUseCase; // Use case for claiming first reward
        private final MakePurchaseForUserWithCreditsUseCase makePurchaseForUserWithCreditsUseCase; // Use case for
                                                                                                   // making purchases
                                                                                                   // with user credits

        // Use case for updating user bio

        public Controller(
                        CreateUserUseCase createUserUseCase,
                        GetUserByUIDUseCase getUserByUIDUseCase,
                        GetUserByPositionUseCase getUserByPositionUseCase,
                        UpdateUserImagesUseCase updateUserImagesUseCase,
                        UpdateUserCharacteristicsUseCase updateUserCharacteristicsUseCase,
                        UpdateUserSettingsUseCase updateUserSettingsUseCase,
                        UpdateUserLocationUseCase updateUserLocationUseCase,

                        DeleteUserUseCase deleteUserUseCase, UpdateBioUseCase updateBioUseCase,
                        UpdateProfileDiscoverySettingsUseCase updateProfilediscoverySettings,
                        UpdateFilterCharacteristicsUseCase updateFilterCharacteristicsUseCase,
                        UpdateAverageRatingUseCase updateAverageRatingUseCase,
                        RenewUserCreditsUseCase renewUserCreditsUseCase,
                        ClaimFirstRewardUseCase claimFirstRewardUseCase,
                        MakePurchaseForUserWithCreditsUseCase makePurchaseForUserWithCreditsUseCase) {
                this.createUserUseCase = createUserUseCase;
                this.getUserByUIDUseCase = getUserByUIDUseCase;
                this.getUserByPositionUseCase = getUserByPositionUseCase;
                this.updateUserImagesUseCase = updateUserImagesUseCase;
                this.updateUserCharacteristicsUseCase = updateUserCharacteristicsUseCase;
                this.updateUserSettingsUseCase = updateUserSettingsUseCase;
                this.updateUserLocationUseCase = updateUserLocationUseCase;
                this.deleteUserUseCase = deleteUserUseCase;
                this.updateBioUseCase = updateBioUseCase;
                this.updateProfilediscoverySettings = updateProfilediscoverySettings; // Initialize the use case for
                this.updateFilterCharacteristicsUseCase = updateFilterCharacteristicsUseCase;
                this.updateAverageRatingUseCase = updateAverageRatingUseCase; // Use case for updating average rating
                                                                              // updating user bio
                // Initialize the use case for updating user bio
                this.renewUserCreditsUseCase = renewUserCreditsUseCase; // Use case for renewing user credits
                this.claimFirstRewardUseCase = claimFirstRewardUseCase; // Use case for claiming first reward
                this.makePurchaseForUserWithCreditsUseCase = makePurchaseForUserWithCreditsUseCase; // Use case for
                                                                                                    // making purchases
                                                                                                    // with user credits
        }

        // CREATE METHODS

        @PostMapping("/create")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> createUser(@RequestBody UserDataModel user) {
                return createUserUseCase.execute(user)
                                .map(savedUser -> ResponseEntity
                                                .ok(ApiResponse.success("User created successfully.", savedUser)));
        }

        /**
         * Método para obtener los datos del usuario a partir de su UID.
         * 
         * Solo devuelve los datos del usuario que lo ha solicitado, no los de otro
         * usuario.
         * 
         * @param userUID El UID del usuario a buscar.
         * @return Un Mono que emite una ResponseEntity con el UserDataModel encontrado.
         */
        @GetMapping("/get")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> getUserData(@RequestHeader("userUID") String userUID) {
                return getUserByUIDUseCase.execute(userUID)
                                .map(userDTO -> ResponseEntity.ok(ApiResponse.success(userDTO)));
        };

        /**
         * Método para obtener un usuario por su UID, devuelve un UserDTO con los datos
         * del usuario.
         * 
         * @param userUID El UID del usuario a buscar.
         * @return Un Mono que emite una ResponseEntity con el UserDTO encontrado.
         */

        @GetMapping("/get/{userUID}")
        public Mono<ResponseEntity<ApiResponse<UserDTO>>> getUser(@PathVariable("userUID") String userUID) {
                return getUserByUIDUseCase.executeWithDTO(userUID)
                                .map(userDTO -> ResponseEntity.ok(ApiResponse.success(userDTO)));
        };

        @GetMapping("/get-users-by-position")
        public Mono<ResponseEntity<ApiResponse<List<UserDTOwithDistance>>>> getUsersByPosition(
                        @RequestParam Map<String, String> allParams) {

                try {
                        // 1. Extraer los parámetros fijos y obligatorios, convirtiéndolos a su tipo.
                        double latitude = Double.parseDouble(allParams.get("latitude"));
                        double longitude = Double.parseDouble(allParams.get("longitude"));
                        double radiusInKm = Double.parseDouble(allParams.get("radiusInKm"));
                        Integer maxAge = Integer.parseInt(allParams.get("maxAge"));
                        Integer minAge = Integer.parseInt(allParams.get("minAge"));
                        String preferredSex = allParams.get("preferredSex");

                        // 2. Crear un mapa solo para las características dinámicas.
                        HashMap<String, Object> characteristics = new HashMap<>();
                        List<String> fixedParams = Arrays.asList("latitude", "longitude", "radiusInKm", "maxAge",
                                        "minAge", "preferredSex");

                        allParams.forEach((key, value) -> {
                                if (!fixedParams.contains(key)) {
                                        characteristics.put(key, value);
                                }
                        });

                        // 3. Llamar al caso de uso con los parámetros ya separados.
                        return getUserByPositionUseCase
                                        .execute(latitude, longitude, radiusInKm, characteristics, maxAge, minAge,
                                                        preferredSex)
                                        .collectList()
                                        .map(userList -> ResponseEntity.ok(ApiResponse.success(userList)));
                } catch (NumberFormatException | NullPointerException e) {
                        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error(
                                        "Missing or invalid required parameters (latitude, longitude, radiusInKm, maxAge, minAge, preferredSex).")));
                }
        }

        @PutMapping("/update-bio")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateBio(@RequestHeader("userUID") String userUID,
                        @RequestBody Map<String, String> bioData) {
                String userBio = bioData.get("userBio");

                // La validación de nulos/vacíos se delega a la capa de validación o al caso de
                // uso.
                // Si userUID o userBio son nulos, el use case o el repositorio lanzarán una
                // excepción
                // que será capturada por GlobalExceptionHandler.
                return updateBioUseCase.execute(userUID, userBio)
                                .map(updatedUser -> ResponseEntity
                                                .ok(ApiResponse.success("User bio updated successfully.",
                                                                updatedUser)));
        }

        @PutMapping("/update-characteristics")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateCharacteristics(
                        @RequestHeader("userUID") String userUID,
                        @RequestBody UserCharacteristicsModel characteristics) {
                return updateUserCharacteristicsUseCase.execute(userUID, characteristics)
                                .map(updatedUser -> ResponseEntity
                                                .ok(ApiResponse.success("User characteristics updated successfully.",
                                                                updatedUser)));
        }

        @PutMapping("/update-settings")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateSettings(@RequestHeader("userUID") String userUID,
                        @RequestBody UserSettingsModel settings) {
                return updateUserSettingsUseCase.execute(userUID, settings)
                                .map(updatedUser -> ResponseEntity
                                                .ok(ApiResponse.success("User settings updated successfully.",
                                                                updatedUser)));
        }

        @PutMapping("/update-images")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateImages(
                        @RequestHeader("userUID") String userUID,
                        @RequestBody Map<String, String> images) {
                return updateUserImagesUseCase.execute(
                                userUID,
                                images.get("userImage1"),
                                images.get("userImage2"),
                                images.get("userImage3"),
                                images.get("userImage4"),
                                images.get("userImage5"),
                                images.get("userImage6"))
                                .map(updatedUser -> ResponseEntity
                                                .ok(ApiResponse.success("User images updated successfully.",
                                                                updatedUser)));
        }

        @PutMapping("/update-location")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateLocation(
                        @RequestHeader("userUID") String userUID,
                        @RequestBody Map<String, Double> locationData) {
                Double longitude = locationData.get("longitude");
                Double latitude = locationData.get("latitude");
                if (longitude == null || latitude == null) {
                        // Devolvemos un error dentro del flujo reactivo para ser manejado por el
                        // exception handler.
                        return Mono.error(new IllegalArgumentException("Longitude and latitude are required."));
                }
                GeoJsonPoint location = new GeoJsonPoint(longitude, latitude);

                return updateUserLocationUseCase.execute(userUID, location)
                                .map(updatedUser -> ResponseEntity
                                                .ok(ApiResponse.success("User location updated successfully.",
                                                                updatedUser)));
        }

        @DeleteMapping("/delete")
        public Mono<ResponseEntity<ApiResponse<Object>>> deleteUser(@RequestParam("userUID") String userUID) {
                return deleteUserUseCase.execute(userUID)
                                .thenReturn(ResponseEntity.ok(ApiResponse.success("User deleted successfully.", null)));
        }

        @PutMapping("/update-discovery-settings")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateDiscoverySettings(
                        @RequestHeader("userUID") String userUID,
                        @RequestBody Map<String, Object> settings) {
                // CORRECCIÓN: El endpoint ahora devuelve el usuario actualizado en la
                // respuesta.
                return updateProfilediscoverySettings.execute(userUID, settings)
                                .map(updatedUser -> ResponseEntity.ok(
                                                ApiResponse.success("Discovery settings updated successfully.",
                                                                updatedUser)));
        }

        @PutMapping("/update-filter-characteristics")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateFilterCharacteristics(
                        @RequestHeader("userUID") String userUID,
                        @RequestBody UpdateFilterCharacteristicsRequest request) {
                return updateFilterCharacteristicsUseCase.execute(
                                userUID,
                                request.getCharacteristics(),
                                request.getMaxAge(),
                                request.getMinAge(),
                                request.getSexPreference(),
                                request.getSearchRadiusInKm())
                                .map(updatedUser -> ResponseEntity.ok(
                                                ApiResponse.success("Filter characteristics updated successfully.",
                                                                updatedUser)));
        }

        @PutMapping("/update-average-rating")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> updateAverageRating(
                        @RequestHeader("userUID") String userUID,
                        @RequestBody Map<String, Integer> averageRatingData) {
                Integer averageRating = averageRatingData.get("averageRating");
                if (averageRating == null) {
                        return Mono.error(new IllegalArgumentException("Average rating is required."));
                }
                return updateAverageRatingUseCase.execute(userUID, averageRating)
                                .map(updatedUser -> ResponseEntity
                                                .ok(ApiResponse.success("Average rating updated successfully.",
                                                                updatedUser)));
        }

        @GetMapping("/renew-user-credits")
        public Mono<ResponseEntity<ApiResponse<Object>>> renewUserCredits(@RequestHeader("userUID") String userUID) {
                return renewUserCreditsUseCase.execute(userUID)
                                .thenReturn(ResponseEntity
                                                .ok(ApiResponse.success("User credits renewed successfully.", null)));
        }

        @PutMapping("/claim-first-reward")
        public Mono<ResponseEntity<ApiResponse<Object>>> claimFirstReward(@RequestHeader("userUID") String userUID) {
                return claimFirstRewardUseCase.execute(userUID)
                                .map(updatedUser -> ResponseEntity.ok(ApiResponse
                                                .success("First reward claimed successfully.", updatedUser)));
        }

        @PostMapping("/make-purchase")
        public Mono<ResponseEntity<ApiResponse<UserDataModel>>> makePurchase(
                        @RequestHeader("userUID") String userUID,
                        @RequestBody Map<String, String> purchaseData) {
                String purchaseType = purchaseData.get("purchaseType");
                if (purchaseType == null) {
                        return Mono.error(new IllegalArgumentException("Purchase type is required."));
                }

                return makePurchaseForUserWithCreditsUseCase.execute(userUID, purchaseType)
                                .map(updatedUser -> ResponseEntity.ok(
                                                ApiResponse.success("Purchase made successfully.", updatedUser)));
        }
}