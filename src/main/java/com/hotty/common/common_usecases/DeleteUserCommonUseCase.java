package com.hotty.common.common_usecases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.auth_service.models.AuthDataModel;
import com.hotty.auth_service.usecases.DeleteUserAuthDataUseCase;
import com.hotty.auth_service.usecases.GetUserAuthDataUseCase;
import com.hotty.auth_service.usecases.LogOutUseCase;
import com.hotty.auth_service.usecases.RestoreUserAuthDataUseCase;
import com.hotty.chat_service.usecases.chat.DeleteChatsByUserUIDUseCase;
import com.hotty.common.common_transactions.MongoTransactionsRepository;
import com.hotty.likes_service.usecases.DeleteAllLikesBYUserUseCase;
import com.hotty.media_service.service.MediaService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.usecases.CreateUserUseCase;
import com.hotty.user_service.usecases.DeleteUserUseCase;
import com.hotty.user_service.usecases.GetUserByUIDUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DeleteUserCommonUseCase {

        private static final Logger log = LoggerFactory.getLogger(DeleteUserCommonUseCase.class);
        private static final List<String> MEDIA_UIDS = List.of("userImage1", "userImage2", "userImage3",
                        "userImage4", "userImage5", "userImage6");

        private final ObjectMapper objectMapper;

        // Repositories and Services
        private final MongoTransactionsRepository transactionsRepository;
        private final MediaService mediaService;

        // Use Cases
        private final GetUserByUIDUseCase getUserByUIDUseCase;
        private final DeleteUserUseCase deleteUserUseCase;
        private final CreateUserUseCase createUserUseCase;
        private final DeleteChatsByUserUIDUseCase deleteChatsByUserUIDUseCase;
        private final DeleteAllLikesBYUserUseCase deleteAllLikesUseCase;
        private final DeleteUserAuthDataUseCase deleteUserAuthDataUseCase;
        private final GetUserAuthDataUseCase getUserAuthDataUseCase;
        private final RestoreUserAuthDataUseCase restoreUserAuthDataUseCase;
        private final LogOutUseCase logOutUseCase;

        public DeleteUserCommonUseCase(
                        MongoTransactionsRepository transactionsRepository,
                        GetUserByUIDUseCase getUserByUIDUseCase,
                        DeleteUserUseCase deleteUserUseCase,
                        CreateUserUseCase createUserUseCase,
                        MediaService mediaService,
                        DeleteChatsByUserUIDUseCase deleteChatsByUserUIDUseCase,
                        DeleteAllLikesBYUserUseCase deleteAllLikesUseCase,
                        DeleteUserAuthDataUseCase deleteUserAuthDataUseCase,
                        GetUserAuthDataUseCase getUserAuthDataUseCase,
                        RestoreUserAuthDataUseCase restoreUserAuthDataUseCase,
                        LogOutUseCase logOutUseCase) {

                this.objectMapper = new ObjectMapper();
                this.transactionsRepository = transactionsRepository;
                this.getUserByUIDUseCase = getUserByUIDUseCase;
                this.deleteUserUseCase = deleteUserUseCase;
                this.createUserUseCase = createUserUseCase;
                this.mediaService = mediaService;
                this.deleteChatsByUserUIDUseCase = deleteChatsByUserUIDUseCase;
                this.deleteAllLikesUseCase = deleteAllLikesUseCase;
                this.deleteUserAuthDataUseCase = deleteUserAuthDataUseCase;
                this.getUserAuthDataUseCase = getUserAuthDataUseCase;
                this.restoreUserAuthDataUseCase = restoreUserAuthDataUseCase;
                this.logOutUseCase = logOutUseCase;
        }

        public Mono<Object> execute(String userUID) {
                log.info("Starting deletion process for userUID: {}", userUID);

                return cacheUserEssentialData(userUID)
                                .flatMap(userCachedData -> {
                                        log.info("User data cached successfully for userUID: {}", userUID);
                                        log.debug("Cached data summary - Auth keys: {}, User keys: {}, Media files: {}",
                                                        userCachedData.authData(),
                                                        userCachedData.userData(),
                                                        userCachedData.mediaData().size());

                                        // Eliminar datos no esenciales PRIMERO (fuera de transacción)
                                        return deleteNonEssentialData(userUID)
                                                        .then(Mono.defer(() -> {
                                                                log.info("Starting essential data deletion for userUID: {}",
                                                                                userUID);
                                                                // Eliminar datos esenciales EN TRANSACCIÓN
                                                                return deleteEssentialDataInTransaction(userUID,
                                                                                userCachedData);
                                                        }))
                                                        .then(logOutUseCase.execute(userUID))
                                                        .thenReturn("User deleted successfully: " + userUID)
                                                        .onErrorResume(error -> {
                                                                log.error("Error occurred during deletion for userUID: {}: {}",
                                                                                userUID, error.getMessage());
                                                                log.info("Starting rollback process for userUID: {}",
                                                                                userUID);

                                                                return performRollBack(userUID, userCachedData)
                                                                                .then(Mono.error(new RuntimeException(
                                                                                                "Error deleting user: "
                                                                                                                + userUID,
                                                                                                error)));
                                                        });
                                });
        }

        private Mono<UserCachedData> cacheUserEssentialData(String userUID) {
                log.info("Starting data caching for userUID: {}", userUID);

                UserCachedData userCachedData = new UserCachedData();

                return getUserAuthDataUseCase.execute(userUID)
                                .doOnNext(authData -> {
                                        log.debug("Auth data retrieved for userUID: {}", userUID);
                                        userCachedData.setAuthData(authData);
                                })
                                .flatMap(authData -> getUserByUIDUseCase.execute(userUID)
                                                .doOnNext(userData -> {
                                                        log.debug("User data retrieved for userUID: {}", userUID);
                                                        userCachedData.setUserData(userData);
                                                })
                                                .flatMap(userData -> cacheMediaFiles(userUID, userCachedData)))
                                .doOnError(error -> log.error("Error caching user essential data for user: {}: {}",
                                                userUID, error.getMessage()));
        }

        private Mono<UserCachedData> cacheMediaFiles(String userUID, UserCachedData userCachedData) {
                log.debug("User data cached for userUID: {}", userUID);

                Map<String, Mono<byte[]>> mediaMonoMap = new HashMap<>();
                Map<String, Object> userDataMap = objectMapper.convertValue(userCachedData.userData(), Map.class);

                for (String mediaUID : MEDIA_UIDS) {
                        Object mediaValue = userDataMap.get(mediaUID);
                        if (mediaValue != null && !mediaValue.toString().isEmpty()) {
                                String filename = mediaValue.toString();
                                log.debug("Found media file: {} -> {} for userUID: {}", mediaUID, filename, userUID);
                                mediaMonoMap.put(mediaUID, mediaService.getFile(filename));
                        }
                }

                if (mediaMonoMap.isEmpty()) {
                        log.debug("No images to process for userUID: {}", userUID);
                        return Mono.just(userCachedData);
                }

                log.debug("Starting download of {} media files for userUID: {}", mediaMonoMap.size(), userUID);

                List<Mono<Map.Entry<String, byte[]>>> entryMonos = mediaMonoMap.entrySet().stream()
                                .map(entry -> entry.getValue()
                                                .map(data -> {
                                                        log.debug("Media downloaded: {} ({} bytes) for userUID: {}",
                                                                        entry.getKey(), data.length, userUID);
                                                        return Map.entry(entry.getKey(), data);
                                                })
                                                .onErrorResume(error -> {
                                                        log.warn("Skipping failed media {} for userUID: {}: {}",
                                                                        entry.getKey(), userUID, error.getMessage());
                                                        return Mono.empty();
                                                }))
                                .toList();

                return Flux.merge(entryMonos)
                                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                                .map(cachedMedia -> {
                                        userCachedData.mediaData().putAll(cachedMedia);
                                        log.info("All media cached successfully: {} files for userUID: {}",
                                                        cachedMedia.size(), userUID);
                                        return userCachedData;
                                });
        }

        private Mono<Void> deleteNonEssentialData(String userUID) {
                log.info("Starting non-essential data deletion for userUID: {}", userUID);

                return transactionsRepository.executeInTransaction(template -> {
                        return deleteChatsByUserUIDUseCase.execute(userUID)
                                        .doOnSuccess(result -> log.debug("Chats deleted for userUID: {}", userUID))
                                        .doOnError(error -> log.error("Error deleting chats for userUID: {}: {}",
                                                        userUID, error.getMessage()))
                                        .then(deleteAllLikesUseCase.execute(userUID))
                                        .doOnSuccess(result -> log.debug("Likes deleted for userUID: {}", userUID))
                                        .doOnError(error -> log.error("Error deleting likes for userUID: {}: {}",
                                                        userUID, error.getMessage()))
                                        .doOnSuccess(v -> log.info(
                                                        "Non-essential data deleted successfully for user: {}",
                                                        userUID))
                                        .doOnError(error -> log.error(
                                                        "Error deleting non-essential data for user: {}: {}",
                                                        userUID, error.getMessage()))
                                        .then();
                });
        }

        private Mono<Void> deleteEssentialDataInTransaction(String userUID, UserCachedData userCachedData) {
                return transactionsRepository.executeInTransaction(template -> {
                        log.debug("Executing essential data deletion in transaction for userUID: {}", userUID);

                        Map<String, Object> userDataMap = objectMapper.convertValue(userCachedData.userData(),
                                        Map.class);

                        List<Mono<Boolean>> deleteMediaMonos = new ArrayList<>();
                        for (String mediaUID : MEDIA_UIDS) {
                                Object mediaValue = userDataMap.get(mediaUID);
                                if (mediaValue != null && !mediaValue.toString().isEmpty()) {
                                        String filename = mediaValue.toString();
                                        log.debug("Deleting media: {} -> {} for userUID: {}", mediaUID, filename,
                                                        userUID);
                                        deleteMediaMonos.add(mediaService.deleteFile(filename));
                                }
                        }

                        return Flux.fromIterable(deleteMediaMonos)
                                        .flatMap(mono -> mono.onErrorResume(error -> {
                                                log.warn("Failed to delete media file, continuing: {}",
                                                                error.getMessage());
                                                return Mono.empty();
                                        }))
                                        .then()
                                        .doOnSuccess(v -> log.debug("All media files deleted for userUID: {}", userUID))
                                        .then(deleteUserUseCase.execute(userUID))
                                        .doOnSuccess(result -> log.debug("User deleted from database for userUID: {}",
                                                        userUID))
                                        .then(deleteUserAuthDataUseCase.execute(userUID))
                                        .doOnSuccess(result -> log.debug("Auth data deleted for userUID: {}", userUID))
                                        .doOnSuccess(v -> log.info("Essential data deleted successfully for user: {}",
                                                        userUID))
                                        .doOnError(error -> log.error("Error deleting essential data for user: {}: {}",
                                                        userUID, error.getMessage()))
                                        .then();
                });
        }

        private Mono<Void> performRollBack(String userUID, UserCachedData userCachedData) {
                log.info("Starting rollback for userUID: {}", userUID);

                return transactionsRepository.executeInTransaction(template -> {
                        return restoreUserAuthDataUseCase.execute(userCachedData.authData())
                                        .doOnSuccess(data -> log.debug(
                                                        "Auth data restored successfully for userUID: {}", userUID))
                                        .then(createUserUseCase.execute(userCachedData.userData()))
                                        .doOnSuccess(user -> log.debug("User recreated successfully for userUID: {}",
                                                        userUID))
                                        .then();
                })
                                .then(restoreMediaFiles(userUID, userCachedData))
                                .doOnSuccess(v -> log.info("Rollback completed successfully for user: {}", userUID))
                                .doOnError(error -> log.error("Error during rollback for user: {}: {}", userUID,
                                                error.getMessage()));
        }

        private Mono<Void> restoreMediaFiles(String userUID, UserCachedData userCachedData) {
                log.debug("Starting media restoration for userUID: {}", userUID);

                Map<String, Object> userDataMap = objectMapper.convertValue(userCachedData.userData(), Map.class);

                List<Mono<String>> restoreMediaMonos = new ArrayList<>();
                for (Map.Entry<String, byte[]> entry : userCachedData.mediaData().entrySet()) {
                        String mediaUID = userDataMap.get(entry.getKey()).toString();
                        byte[] mediaData = entry.getValue();
                        log.debug("Restoring media: {} ({} bytes) for userUID: {}", mediaUID, mediaData.length,
                                        userUID);
                        restoreMediaMonos.add(mediaService.uploadFileFromBytes(mediaUID, mediaData,
                                        "application/octet-stream"));
                }

                return Flux.merge(restoreMediaMonos)
                                .doOnNext(result -> log.debug("Media file restored: {} for userUID: {}", result,
                                                userUID))
                                .then()
                                .doOnSuccess(v -> log.debug("All media files restored for userUID: {}", userUID));
        }

        private static class UserCachedData {
                private AuthDataModel authData;
                private UserDataModel userData;
                private Map<String, byte[]> mediaData;

                public UserCachedData() {
                        this.authData = new AuthDataModel();
                        this.userData = new UserDataModel();
                        this.mediaData = new HashMap<>();
                }

                public UserCachedData(AuthDataModel authData, UserDataModel userData, Map<String, byte[]> mediaData) {
                        this.authData = authData;
                        this.userData = userData;
                        this.mediaData = mediaData;
                }

                public AuthDataModel authData() {
                        return authData;
                }

                public UserDataModel userData() {
                        return userData;
                }

                public Map<String, byte[]> mediaData() {
                        return mediaData;
                }

                public void setAuthData(AuthDataModel authData) {
                        this.authData = authData;
                }

                public void setUserData(UserDataModel userData) {
                        this.userData = userData;
                }

                public void setMediaData(Map<String, byte[]> mediaData) {
                        this.mediaData = mediaData;
                }
        }
}
