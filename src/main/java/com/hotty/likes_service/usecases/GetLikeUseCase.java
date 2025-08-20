package com.hotty.likes_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.likes_service.model.LikeModel;
import com.hotty.likes_service.repository.LikesRepo;

import reactor.core.publisher.Mono;

/**
 * Caso de uso para obtener un "like" específico por su UID.
 * Utiliza el repositorio LikesRepo para acceder a la base de datos.
 */
@Component
public class GetLikeUseCase {

    private final LikesRepo likesRepository;

    public GetLikeUseCase(LikesRepo likesRepository) {
        this.likesRepository = likesRepository;
    }

    public Mono<LikeModel> execute(String likeUID) {
        return likesRepository.findByLikeUID(likeUID);
    }

}
