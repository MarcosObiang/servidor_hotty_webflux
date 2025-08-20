package com.hotty.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotty.auth_service.models.AuthTokenDataModel;

/**
 * Configuración común de Redis para todo el monolito.
 * Soporte tanto para String-String como para modelos específicos.
 */
@Configuration
public class RedisConfig {

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext()
                .key(stringRedisSerializer)
                .value(stringRedisSerializer)
                .hashKey(stringRedisSerializer)
                .hashValue(stringRedisSerializer)
                .build();

        ReactiveRedisTemplate<String, String> template = new ReactiveRedisTemplate<>(
                reactiveRedisConnectionFactory, context);
        
        return template;
    }

    @Bean
    public ReactiveRedisTemplate<String, AuthTokenDataModel> authTokenReactiveRedisTemplate(
            ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        
        // Configurar ObjectMapper para AuthTokenDataModel
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JavaTimeModule());
        
        Jackson2JsonRedisSerializer<AuthTokenDataModel> jackson2JsonRedisSerializer = 
                new Jackson2JsonRedisSerializer<>(objectMapper, AuthTokenDataModel.class);
        
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        RedisSerializationContext<String, AuthTokenDataModel> context = RedisSerializationContext
                .<String, AuthTokenDataModel>newSerializationContext()
                .key(stringRedisSerializer)
                .value(jackson2JsonRedisSerializer)
                .hashKey(stringRedisSerializer)
                .hashValue(jackson2JsonRedisSerializer)
                .build();

        return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory, context);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Configuración de serializadores
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setHashValueSerializer(new JdkSerializationRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}
