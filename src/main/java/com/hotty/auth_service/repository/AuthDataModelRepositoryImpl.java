package com.hotty.auth_service.repository;

import java.util.NoSuchElementException;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import com.hotty.auth_service.Exceptions.NotFoundExceptionCustom;
import com.hotty.auth_service.interfaces.AuthDataModelRepository;
import com.hotty.auth_service.models.AuthDataModel;

import reactor.core.publisher.Mono;

@Repository
public class AuthDataModelRepositoryImpl implements AuthDataModelRepository {

    ReactiveMongoTemplate mongoTemplate;

    public AuthDataModelRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<AuthDataModel> findUserByUID(String userUID) {

        Query query = new Query();
        query.addCriteria(Criteria.where("userUID").is(userUID));
        return mongoTemplate.findOne(query, AuthDataModel.class).switchIfEmpty(
                Mono.error(new NoSuchElementException("No se ha encontrado el usuario con el UID especificado." + userUID)));

    }

    @Override
    public Mono<AuthDataModel> findByEmail(String email) {
        System.out.println("=== DEBUG FIND BY EMAIL ===");
        System.out.println("Email buscado: '" + email + "'");
        
        Query query = Query.query(Criteria.where("email").is(email));
        
        return mongoTemplate.findOne(query, AuthDataModel.class)
            .switchIfEmpty(
                Mono.defer(() -> {
                    System.out.println("*** switchIfEmpty activado - no se encontró usuario");
                    return Mono.error(new NoSuchElementException("No se ha encontrado el usuario con el email especificado: " + email));
                })
            );
    }

    @Override
    public Mono<Void> deleteByUserUID(String userUID) {
        Query query = new Query();
        Criteria criteria = Criteria.where("userUID").is(userUID);
        query.addCriteria(criteria);
        return mongoTemplate.remove(query, AuthDataModel.class).then();

    }

    @Override
    public Mono<AuthDataModel> save(AuthDataModel authDataModel) {

        Query query = new Query();

        Criteria criteria = Criteria.where("userUID").is(authDataModel.getUserUID());
        query.addCriteria(criteria);
       return mongoTemplate.findOne(query, AuthDataModel.class)
            .flatMap(existingAuthDataModel -> {
                // Si existe, actualiza los campos
                existingAuthDataModel.setEmail(authDataModel.getEmail());
                existingAuthDataModel.setAuthProvider(authDataModel.getAuthProvider());
                existingAuthDataModel.setIsUserRegisteredAlready(authDataModel.getIsUserRegisteredAlready());
                // No actualices created_at, y updated_at se actualiza automáticamente
                return mongoTemplate.save(existingAuthDataModel);
            })
            .switchIfEmpty(
                // Si no existe, inserta uno nuevo
                mongoTemplate.save(authDataModel)
            );

    }

}
