package com.hotty.auth_service.repository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.hotty.auth_service.models.AuthTokenDataModel;

@Repository
public class AuthServiceCacheRepository {


    // private RedisTemplate<String, Object> redisTemplate;

    // public AuthServiceCacheRepository(RedisTemplate<String, Object> redisTemplate) {
    //     this.redisTemplate = redisTemplate;
    // }
    // public void save(AuthTokenDataModel authTokenDataModel) {
    //     // Crear el mapa con los datos que se guardarán en Redis
    //     HashMap<String, Object> value = new HashMap<>();
    //     value.put("userUID", authTokenDataModel.getUserUID());
    //     value.put("tokenUID", authTokenDataModel.getTokenUID());
    //     value.put("expTime", authTokenDataModel.getExpiresAt());
    //     value.put("isRevoked", authTokenDataModel.isRevoked());
    
    //     // Calcular el TTL en segundos de manera más limpia
    //     long ttlInSeconds = Duration.between(Instant.now(), 
    //             authTokenDataModel.getExpiresAt()).getSeconds();
    
    //     // Establecer el TTL para la clave en Redis
    //     redisTemplate.expire(authTokenDataModel.getToken(), ttlInSeconds, TimeUnit.SECONDS);
    
    //     // Guardar los datos en Redis usando Hash
    //     redisTemplate.opsForHash().putAll(authTokenDataModel.getToken(), value);
    // }
    



    
}
