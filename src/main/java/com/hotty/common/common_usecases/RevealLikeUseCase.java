package com.hotty.common.common_usecases;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.common.exceptions.DownstreamServiceException;
import com.hotty.likes_service.usecases.GetLikeUseCase;
import com.hotty.likes_service.usecases.UpdateLikeUseCase;
import com.hotty.user_service.usecases.MakePurchaseForUserWithCreditsUseCase;

import reactor.core.publisher.Mono;

@Component
public class RevealLikeUseCase {

    private final GetLikeUseCase getLikeUseCase;
    private final MakePurchaseForUserWithCreditsUseCase makePurchaseForUserWithCreditsUseCase;
    private final ObjectMapper objectMapper;
    private final UpdateLikeUseCase updateLikeUseCase;

    public RevealLikeUseCase(GetLikeUseCase getLikeUseCase,
            MakePurchaseForUserWithCreditsUseCase makePurchaseForUserWithCreditsUseCase, ObjectMapper objectMapper,
            UpdateLikeUseCase updateLikeUseCase) {
        this.getLikeUseCase = getLikeUseCase;
        this.makePurchaseForUserWithCreditsUseCase = makePurchaseForUserWithCreditsUseCase;
        this.objectMapper = objectMapper;
        this.updateLikeUseCase = updateLikeUseCase;
    }

    public Mono<Object> execute(String likeUID, String userUID) {
        Map<String, String> purchaseData = new HashMap<>();
        purchaseData.put("purchaseType", "REACTION_REVELATION");

        // 1. Obtener el like
        return getLikeUseCase.execute(likeUID)
                .onErrorResume(WebClientResponseException.class,
                        ex -> Mono.error(new DownstreamServiceException("Error getting like from likes-service.",
                                ex.getRawStatusCode(), ex.getResponseBodyAsString())))
                .flatMap(likeData -> {
                    String senderUID = (String) likeData.getSenderUID();
                    if (senderUID == null) {
                        return Mono.error(new IllegalArgumentException("Like data is missing senderUID."));
                    }

                    return makePurchaseForUserWithCreditsUseCase.execute(senderUID, "REACTION_REVELATION")
                            .onErrorResume(WebClientResponseException.class,
                                    ex -> Mono.error(
                                            new DownstreamServiceException("Error getting user data from user-service.",
                                                    ex.getRawStatusCode(), ex.getResponseBodyAsString())))
                            .flatMap(userData -> {

                                String senderName = (String) userData.getName();
                                String senderPictureUrl = (String) userData.getUserImage1();

                                if (senderName == null || senderPictureUrl == null || userData.getBirthDate() == null) {
                                    return Mono.error(
                                            new IllegalArgumentException("User data from user-service is incomplete."));
                                }

                                // 3. Actualizar el like con la información revelada
                                return updateLikeUseCase
                                        .execute(likeUID, userUID, true, senderPictureUrl, userData.getBirthDate(),
                                                senderName)
                                        .onErrorResume(WebClientResponseException.class,
                                                ex -> Mono.error(new DownstreamServiceException(
                                                        "Error updating like in likes-service.", ex.getRawStatusCode(),
                                                        ex.getResponseBodyAsString())));
                            });
                });
    }
}
