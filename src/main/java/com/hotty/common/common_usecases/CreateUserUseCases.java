package com.hotty.common.common_usecases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.media_service.service.MediaService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.usecases.CreateUserUseCase;
import com.hotty.user_service.usecases.DeleteUserUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreateUserUseCases {

        private static final Logger log = LoggerFactory.getLogger(CreateUserUseCases.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        private final MediaService mediaService;
        private final CreateUserUseCase createUserUseCase;
        private final DeleteUserUseCase deleteUserUseCase;

        public CreateUserUseCases(MediaService mediaService,
                        CreateUserUseCase createUserUseCase,
                        DeleteUserUseCase deleteUserUseCase) {
                this.mediaService = mediaService;
                this.createUserUseCase = createUserUseCase;
                this.deleteUserUseCase = deleteUserUseCase;
        }

        public Mono<String> execute(String userUID, String userJson,
                        FilePart userImage1, FilePart userImage2,
                        FilePart userImage3, FilePart userImage4,
                        FilePart userImage5, FilePart userImage6)
                        throws JsonMappingException, JsonProcessingException {

                log.info("Creating user with UID: {}", userUID);

                // 1. Validar y preparar datos del usuario
                Map<String, Object> userMap = prepareUserData(userUID, userJson);

                // 2. Validar imagen obligatoria
                if (userImage1 == null) {
                        return Mono.just("La primera imagen del usuario es obligatoria.");
                }

                // 3. Filtrar imágenes válidas
                List<FilePart> validImages = getValidImages(userImage1, userImage2, userImage3,
                                userImage4, userImage5, userImage6);

                // 4. Ejecutar flujo principal: subir imágenes → crear usuario
                return uploadImagesAndCreateUser(userUID, userMap, validImages)
                                .onErrorResume(error -> handleErrorWithRollback(userUID, error));
        }

        /**
         * Prepara y valida los datos del usuario desde JSON
         */
        private Map<String, Object> prepareUserData(String userUID, String userJson)
                        throws JsonProcessingException {

                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = objectMapper.readValue(userJson, Map.class);
                userMap.put("userUID", userUID);
                userMap.remove("id");

                @SuppressWarnings("unchecked")
                Map<String, Object> characteristicsMap = userMap.get("characteristics") instanceof Map
                                ? (Map<String, Object>) userMap.get("characteristics")
                                : null;

                @SuppressWarnings("unchecked")
                Map<String, Object> settingsMap = userMap.get("settings") instanceof Map
                                ? (Map<String, Object>) userMap.get("settings")
                                : null;

                log.debug("User data prepared - Settings: {}, Characteristics: {}",
                                settingsMap, characteristicsMap);

                return userMap;
        }

        /**
         * Filtra las imágenes válidas (no nulas)
         */
        private List<FilePart> getValidImages(FilePart... images) {
                return Stream.of(images)
                                .filter(image -> image != null)
                                .toList();
        }

        /**
         * Sube las imágenes y crea el usuario con las URLs resultantes
         */
        private Mono<String> uploadImagesAndCreateUser(String userUID,
                        Map<String, Object> userMap,
                        List<FilePart> images) {
                return uploadImages(userUID, images)
                                .flatMap(imageUrls -> createUserWithImages(userMap, imageUrls))
                                .thenReturn("OK");
        }

        /**
         * Sube todas las imágenes en paralelo
         */
        private Mono<List<String>> uploadImages(String userUID, List<FilePart> images) {
                log.debug("Uploading {} images for user: {}", images.size(), userUID);

                List<Mono<String>> uploadMonos = new ArrayList<>();
                for (int i = 0; i < images.size(); i++) {
                        final int imageIndex = i + 1;

                        Mono<String> uploadMono = mediaService
                                        .uploadFile(images.get(i), userUID, imageIndex,
                                                        userUID + "-profileImage-" + imageIndex)
                                        .doOnSuccess(url -> log.debug("Image {} uploaded successfully", imageIndex))
                                        .onErrorMap(WebClientResponseException.class,
                                                        ex -> new RuntimeException(
                                                                        "Error uploading image " + imageIndex +
                                                                                        " to media service",
                                                                        ex));

                        uploadMonos.add(uploadMono);
                }

                return Flux.fromIterable(uploadMonos)
                                .flatMap(mono -> mono) // Execute uploads in parallel
                                .collectList()
                                .doOnSuccess(urls -> log.debug("All {} images uploaded successfully", urls.size()));
        }

        /**
         * Crea el usuario asignando las URLs de las imágenes
         */
        private Mono<UserDataModel> createUserWithImages(Map<String, Object> userMap,
                        List<String> imageUrls) {
                // Limpiar campos de imagen y asignar URLs subidas
                clearImageFields(userMap);
                assignImageUrls(userMap, imageUrls);

                log.debug("Creating user with data: {}", userMap);

                UserDataModel userDataModel = objectMapper.convertValue(userMap, UserDataModel.class);

                return createUserUseCase.execute(userDataModel)
                                .doOnSuccess(user -> log.info("User created successfully: {}", user.getUserUID()))
                                .onErrorMap(WebClientResponseException.class,
                                                ex -> new Exception("Error creating user in user service. Status: " +
                                                                ex.getStatusCode() + ", Response: " +
                                                                ex.getResponseBodyAsString(), ex));
        }

        /**
         * Limpia todos los campos de imagen del mapa de usuario
         */
        private void clearImageFields(Map<String, Object> userMap) {
                for (int i = 1; i <= 6; i++) {
                        userMap.put("userImage" + i, "");
                }
        }

        /**
         * Asigna las URLs de imágenes subidas al mapa de usuario
         */
        private void assignImageUrls(Map<String, Object> userMap, List<String> imageUrls) {
                for (int i = 0; i < imageUrls.size(); i++) {
                        userMap.put("userImage" + (i + 1), imageUrls.get(i));
                }
        }

        /**
         * Maneja errores ejecutando rollback del usuario creado
         */
        private Mono<String> handleErrorWithRollback(String userUID, Throwable error) {
                log.error("Error creating user: {}. Executing rollback...", error.getMessage());

                return deleteUserUseCase.execute(userUID)
                                .doOnSuccess(v -> log.info("User rollback completed for: {}", userUID))
                                .onErrorMap(WebClientResponseException.class,
                                                ex -> new Exception(
                                                                "Error during user rollback in user service: Status " +
                                                                                ex.getStatusCode() + ", Response: " +
                                                                                ex.getResponseBodyAsString()))
                                .then(Mono.error(error)); // Propagar el error original después del rollback
        }
}
