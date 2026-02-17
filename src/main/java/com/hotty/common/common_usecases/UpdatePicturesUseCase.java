package com.hotty.common.common_usecases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import com.hotty.chat_service.usecases.UpdateUserPictureInChatsUseCase;
import com.hotty.common.common_transactions.MongoTransactionsRepository;
import com.hotty.likes_service.usecases.UpdateUserPictureInLikesUseCase;
import com.hotty.media_service.service.MediaService;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.usecases.UpdateUserImagesUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UpdatePicturesUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdatePicturesUseCase.class);

    private final MediaService mediaService;
    private final UpdateUserImagesUseCase updateUserImagesUseCase;
    private final UpdateUserPictureInLikesUseCase updateUserPictureInLikesUseCase;
    private final UpdateUserPictureInChatsUseCase updateUserPictureInChatsUseCase;
    private final MongoTransactionsRepository transactionsRepository;

    public UpdatePicturesUseCase(MediaService mediaService, UpdateUserImagesUseCase updateUserImagesUseCase,
            UpdateUserPictureInLikesUseCase updateUserPictureInLikesUseCase,
            UpdateUserPictureInChatsUseCase updateUserPictureInChatsUseCase,
            MongoTransactionsRepository transactionsRepository) {
        this.mediaService = mediaService;
        this.updateUserImagesUseCase = updateUserImagesUseCase;
        this.updateUserPictureInLikesUseCase = updateUserPictureInLikesUseCase;
        this.updateUserPictureInChatsUseCase = updateUserPictureInChatsUseCase;
        this.transactionsRepository = transactionsRepository;
    }

    public Mono<UserDataModel> execute(String userUID, FilePart userImage1, FilePart userImage2,
            FilePart userImage3, FilePart userImage4, FilePart userImage5, FilePart userImage6) {

        List<FilePart> images = Stream
                .of(userImage1, userImage2, userImage3, userImage4, userImage5, userImage6)
                .filter(fp -> fp != null)
                .toList();
        List<Mono<String>> uploadMonos = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            FilePart image = images.get(i);
            String pictureUid= UUID.randomUUID().toString();
            Mono<String> uploadMono = mediaService.uploadFile(image, userUID, (i + 1),
                    userUID + "-profileImage-" + (i + 1)+ "-" + pictureUid);
            uploadMonos.add(uploadMono);
        }

        return Flux.fromIterable(uploadMonos)
                .flatMap(mono -> mono)
                .collectList().flatMap(imageUrls -> {

                    imageUrls.sort((a, b) -> {
                        try {
                            int numA = Integer.parseInt(a.substring(a.lastIndexOf('-') + 1));
                            int numB = Integer.parseInt(b.substring(b.lastIndexOf('-') + 1));
                            return Integer.compare(numA, numB);
                        } catch (Exception e) {
                            return 0;
                        }
                    });

                    for (int a = 0; a < 6; a++) {
                        if (imageUrls.size() <= a) {
                            imageUrls.add(""); // Fill missing images with empty strings
                        }
                    }

                    Map<String, String> imageUrlMap = new HashMap<>();
                    for (int i = 0; i < imageUrls.size(); i++) {
                        imageUrlMap.put("userImage" + (i + 1), imageUrls.get(i));
                    }

                    String userImage1String = imageUrlMap.get("userImage1");
                    String userImage2String = imageUrlMap.get("userImage2");
                    String userImage3String = imageUrlMap.get("userImage3");
                    String userImage4String = imageUrlMap.get("userImage4");
                    String userImage5String = imageUrlMap.get("userImage5");
                    String userImage6String = imageUrlMap.get("userImage6");

                    // ✅ TRANSACCIÓN: Actualizar usuario, chats y likes
                    return transactionsRepository.executeInTransaction(template -> {
                        log.info("🔄 Starting transaction to update pictures for user: {}", userUID);
                        
                        // 1. Actualizar las imágenes del usuario
                        return updateUserImagesUseCase.execute(userUID, userImage1String, userImage2String,
                                userImage3String, userImage4String, userImage5String, userImage6String)
                                .doOnSuccess(updatedUser -> 
                                    log.debug("✅ User images updated for: {}", userUID))
                                
                                // 2. Actualizar imagen en todos los chats donde aparece el usuario
                                .flatMap(updatedUser -> {
                                    // Usar la primera imagen como imagen de perfil para chats y likes
                                    String profilePictureUrl = userImage1String != null && !userImage1String.isEmpty() 
                                        ? userImage1String 
                                        : ""; // Imagen por defecto o vacía
                                        
                                    return updateUserPictureInChatsUseCase.execute(userUID, profilePictureUrl)
                                        .doOnSuccess(chatCount -> 
                                            log.debug("✅ Updated {} chats for user: {}", chatCount, userUID))
                                        .map(chatCount -> updatedUser); // Retornar el usuario actualizado
                                })
                                
                                // 3. Actualizar imagen en todos los likes donde es sender
                                .flatMap(updatedUser -> {
                                    String profilePictureUrl = userImage1String != null && !userImage1String.isEmpty() 
                                        ? userImage1String 
                                        : "";
                                        
                                    return updateUserPictureInLikesUseCase.execute(userUID, profilePictureUrl)
                                        .doOnSuccess(likeCount -> 
                                            log.debug("✅ Updated {} likes for sender: {}", likeCount, userUID))
                                        .map(likeCount -> updatedUser); // Retornar el usuario actualizado
                                })
                                
                                .doOnSuccess(result -> 
                                    log.info("🎉 Transaction completed successfully for user: {}", userUID))
                                .doOnError(error -> 
                                    log.error("💥 Transaction failed for user: {}. Error: {}", userUID, error.getMessage()));
                    });
                });
    }
}
