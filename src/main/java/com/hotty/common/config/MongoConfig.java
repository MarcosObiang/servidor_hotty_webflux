package com.hotty.common.config;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.hotty.auth_service.models.AuthDataModel;
import com.hotty.auth_service.models.AuthTokenDataModel;
import com.hotty.chat_service.model.ChatModel;
import com.hotty.chat_service.model.MessageModel;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.user_service.model.UserDataModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Configuración común de MongoDB para todo el monolito.
 * Habilita auditoría reactiva, transacciones y crea colecciones automáticamente al inicio.
 */
@Configuration
@EnableReactiveMongoAuditing
@EnableTransactionManagement
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    /**
     * Bean para el manejador de transacciones reactivas de MongoDB.
     * Necesario para usar @Transactional con programación reactiva.
     */
    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTransactionManager(factory);
    }

    /**
     * CommandLineRunner que se ejecuta al iniciar la aplicación para crear
     * las colecciones de MongoDB si no existen.
     */
    @Bean
    public CommandLineRunner initializeCollections(
            ReactiveMongoTemplate mongoTemplate,
            MongoMappingContext mongoMappingContext) {
        
        return args -> {
            log.info("Initializing MongoDB collections...");

            // Lista de todas las clases de modelo
            Class<?>[] modelClasses = {
                // Auth Service
                AuthDataModel.class,
                AuthTokenDataModel.class,
                
                // User Service  
                UserDataModel.class,
                
                // Chat Service
                ChatModel.class,
                MessageModel.class,
                
                // Likes Service
                LikeModel.class
            };

            // Crear colecciones e índices para cada modelo
            Flux.fromArray(modelClasses)
                .flatMap(modelClass -> createCollectionAndIndexes(mongoTemplate, mongoMappingContext, modelClass))
                .blockLast(); // Bloquear hasta que todas las operaciones terminen

            log.info("MongoDB collections initialization completed successfully");
        };
    }

    /**
     * Crea una colección e índices para una clase de modelo específica.
     */
    private Mono<Void> createCollectionAndIndexes(
            ReactiveMongoTemplate mongoTemplate,
            MongoMappingContext mongoMappingContext,
            Class<?> modelClass) {
        
        String collectionName = mongoTemplate.getCollectionName(modelClass);
        
        return mongoTemplate.collectionExists(collectionName)
            .flatMap(exists -> {
                if (!exists) {
                    log.info("Creating collection: {}", collectionName);
                    return mongoTemplate.createCollection(collectionName)
                        .doOnSuccess(collection -> log.debug("Collection created successfully: {}", collectionName))
                        .then(createIndexesForCollection(mongoTemplate, mongoMappingContext, modelClass));
                } else {
                    log.debug("Collection already exists: {}", collectionName);
                    return createIndexesForCollection(mongoTemplate, mongoMappingContext, modelClass);
                }
            })
            .doOnError(error -> log.error("Error creating collection {}: {}", collectionName, error.getMessage()))
            .onErrorResume(error -> {
                log.warn("Continuing despite error with collection {}: {}", collectionName, error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Crea los índices definidos en las anotaciones de una clase de modelo.
     */
    private Mono<Void> createIndexesForCollection(
            ReactiveMongoTemplate mongoTemplate,
            MongoMappingContext mongoMappingContext,
            Class<?> modelClass) {
        
        try {
            ReactiveIndexOperations indexOps = mongoTemplate.indexOps(modelClass);
            MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
            
            return Flux.fromIterable(resolver.resolveIndexFor(modelClass))
                .filter(indexDefinition -> {
                    // Filtrar el índice _id automático para evitar conflictos
                    String indexName = Optional.ofNullable(indexDefinition.getIndexOptions())
                        .map(options -> options.getString("name"))
                        .orElse("");
                    
                    // Skip _id index ya que MongoDB lo crea automáticamente
                    if ("_id".equals(indexName) || "_id_".equals(indexName)) {
                        log.debug("Skipping automatic _id index for {}", modelClass.getSimpleName());
                        return false;
                    }
                    
                    // También filtrar por las keys del índice
                    return !indexDefinition.getIndexKeys().containsKey("_id");
                })
                .flatMap(indexDefinition -> {
                    String indexName = Optional.ofNullable(indexDefinition.getIndexOptions())
                        .map(options -> options.getString("name"))
                        .orElse("auto-generated");
                    
                    return indexOps.ensureIndex(indexDefinition)
                        .doOnNext(name -> log.debug("Index ensured for {}: {}", modelClass.getSimpleName(), name))
                        .onErrorResume(error -> {
                            log.warn("Error creating index {} for {}: {}", 
                                indexName, modelClass.getSimpleName(), error.getMessage());
                            return Mono.empty();
                        });
                })
                .then()
                .doOnSuccess(v -> log.debug("All indexes processed for: {}", modelClass.getSimpleName()));
                
        } catch (Exception e) {
            log.warn("Error resolving indexes for {}: {}", modelClass.getSimpleName(), e.getMessage());
            return Mono.empty();
        }
    }
}
