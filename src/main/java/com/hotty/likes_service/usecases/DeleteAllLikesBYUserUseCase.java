package com.hotty.likes_service.usecases;

import org.springframework.stereotype.Service;

import com.hotty.likes_service.repository.LikesRepo;

import reactor.core.publisher.Mono;


@Service
public class DeleteAllLikesBYUserUseCase {

    private final LikesRepo likesRepo;

    public DeleteAllLikesBYUserUseCase(LikesRepo likesRepo) {
        this.likesRepo = likesRepo;
    }

    public Mono<Long> execute(String userUID) {
        return likesRepo.deleteAll(userUID);
    }

}
