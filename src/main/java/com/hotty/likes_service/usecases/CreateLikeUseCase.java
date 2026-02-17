package com.hotty.likes_service.usecases;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hotty.common.enums.NotificationDataType;
import com.hotty.common.enums.PublishEventType;
import com.hotty.common.services.EventPublishers.LikeEventPublisher;
import com.hotty.common.services.PushNotifications.Factories.NotificationStrategyFactory;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.likes_service.repository.LikesRepo;
import com.hotty.user_service.model.UserNotificationDataModel;
import com.hotty.user_service.usecases.GetUserByUIDUseCase;

import reactor.core.publisher.Mono;

@Service
public class CreateLikeUseCase {

    private final LikesRepo likesRepo;
    private final LikeEventPublisher publisher;
    private final GetUserByUIDUseCase getUserByUIDUseCase;
    private final NotificationStrategyFactory notificationStrategyFactory;

    public CreateLikeUseCase(LikesRepo likesRepo, LikeEventPublisher publisher, GetUserByUIDUseCase getUserByUIDUseCase,
            NotificationStrategyFactory notificationStrategyFactory) {
        this.publisher = publisher;
        this.likesRepo = likesRepo;
        this.getUserByUIDUseCase = getUserByUIDUseCase;
        this.notificationStrategyFactory = notificationStrategyFactory;
    }

    public Mono<LikeModel> execute(String senderUID, String receiverUID, Integer likeValue) {

        // COMENTADO PARA PRUEBAS

        // if (senderUID == null || receiverUID == null ||
        // senderUID.equals(receiverUID)) {
        // return Mono.error(new IllegalArgumentException("El emisor y el receptor no
        // pueden ser nulos o iguales."));
        // }
        Instant now = Instant.now();
        LikeModel like = new LikeModel();
        like.setLikeUID(UUID.randomUUID().toString());
        like.setSenderUID(senderUID);
        like.setReceiverUID(receiverUID);
        like.setCreatedAt(now);
        like.setOfferExpirationDate(now.plusSeconds(86400)); // Expira en 2 minutos ***IMPORTANTE**** CAMBIAR EN PRODUCCION
                                                          // A 24 HORAS (86400 segundos) o (60*60*24)
        like.setLikeValue(likeValue);

        return getUserByUIDUseCase.execute(receiverUID) // Verifica que el usuario receptor exista
                .flatMap(user -> likesRepo.add(like)

                        .flatMap(savedLike -> {
                            return Mono.zip(
                                    publisher.publishLikeCreated(savedLike),
                                    notificationStrategyFactory.getStrategy(user.getNotificationData().getProvider())
                                            .sendNotification(NotificationDataType.LIKE, user.getNotificationData()))
                                    .thenReturn(savedLike);
                        }));
    }

}