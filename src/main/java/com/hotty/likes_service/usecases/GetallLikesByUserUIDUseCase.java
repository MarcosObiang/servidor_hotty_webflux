package com.hotty.likes_service.usecases;

import org.springframework.stereotype.Service;

import com.hotty.likes_service.model.LikeModel;
import com.hotty.likes_service.repository.LikesRepo;

import reactor.core.publisher.Flux;

@Service
public class GetallLikesByUserUIDUseCase {

    private final LikesRepo likesRepo;

    public GetallLikesByUserUIDUseCase(LikesRepo likesRepo) {
        this.likesRepo = likesRepo;

    }

    public Flux<LikeModel> execute(String userUID) {

        return likesRepo.getAll(userUID);

    }
}