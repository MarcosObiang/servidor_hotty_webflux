package com.hotty.likes_service.usecases;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hotty.common.enums.PublishEventType;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.likes_service.repository.LikesRepo;
import com.hotty.common.services.LikeEventPublisher;

import reactor.core.publisher.Mono;

@Service
public class CreateLikeUseCase {

    private final LikesRepo likesRepo;
    private final LikeEventPublisher publisher;

    public CreateLikeUseCase(LikesRepo likesRepo, LikeEventPublisher publisher) {
        this.publisher = publisher;
        this.likesRepo = likesRepo;
    }

    public Mono<LikeModel> execute(String senderUID, String receiverUID, Integer likeValue) {


        //COMENTADO PARA PRUEBAS

        // if (senderUID == null || receiverUID == null || senderUID.equals(receiverUID)) {
        //     return Mono.error(new IllegalArgumentException("El emisor y el receptor no pueden ser nulos o iguales."));
        // }
        Instant now = Instant.now();
        LikeModel like = new LikeModel();
        like.setLikeUID(UUID.randomUUID().toString());
        like.setSenderUID(senderUID);
        like.setReceiverUID(receiverUID);
        like.setCreatedAt(now);
        like.setOfferExpirationDate(now.plusSeconds(10)); // Expira en 2 minutos ***IMPORTANTE**** CAMBIAR EN PRODUCCION A 24 HORAS (86400 segundos) o (60*60*24)
        like.setLikeValue(likeValue);

        return likesRepo.add(like)
                .flatMap(savedLike ->
                        // Encadenamos la publicación del evento.
                        // .thenReturn(savedLike) asegura que devolvemos el objeto original después de publicar.
                        publisher.publishUserCreated(savedLike).thenReturn(savedLike));
    }

}