package com.hotty.common.common_usecases;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hotty.common.common_transactions.MongoTransactionsRepository;
import com.hotty.common.common_transactions.TransactionRetryHelper;
import com.hotty.likes_service.usecases.CreateLikeUseCase;
import com.hotty.user_service.usecases.UpdateAverageRatingUseCase;
import com.hotty.likes_service.model.LikeModel;

import reactor.core.publisher.Mono;

@Component
public class CreateLikeCommonUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateLikeCommonUseCase.class);

    private final MongoTransactionsRepository transactionsRepository;
    private final TransactionRetryHelper retryHelper;
    private final CreateLikeUseCase createLikeUseCase;
    private final UpdateAverageRatingUseCase updateAverageRatingUseCase;

    public CreateLikeCommonUseCase(MongoTransactionsRepository transactionsRepository,
                                 TransactionRetryHelper retryHelper,
                                 CreateLikeUseCase createLikeUseCase,
                                 UpdateAverageRatingUseCase updateAverageRatingUseCase) {
        this.transactionsRepository = transactionsRepository;
        this.retryHelper = retryHelper;
        this.createLikeUseCase = createLikeUseCase;
        this.updateAverageRatingUseCase = updateAverageRatingUseCase;
    }

    /**
     * Crea un nuevo like y actualiza el rating del usuario receptor.
     * Ambas operaciones se ejecutan en una sola transacción.
     *
     * @param userUID    El UID del usuario que da el like.
     * @param likeData   Un mapa que contiene receiverUID y likeValue.
     * @return Un Mono del LikeModel creado.
     */
    public Mono<LikeModel> execute(String userUID, Map<String, Object> likeData) {
        log.info("Creating like from user: {} with data: {}", userUID, likeData);

        // Validaciones iniciales
        String receiverUID = (String) likeData.get("receiverUID");
        Integer likeValue = (Integer) likeData.get("likeValue");

        if (receiverUID == null || likeValue == null) {
            return Mono.error(new IllegalArgumentException("receiverUID and likeValue must not be null."));
        }


        /// Luego de probar, descomentar este chequeo para evitar que un usuario se de like a si mismo

        // if (userUID.equals(receiverUID)) {
        //     return Mono.error(new IllegalArgumentException("User cannot like themselves."));
        // }

        if (likeValue < 0 || likeValue > 100) {
            return Mono.error(new IllegalArgumentException("likeValue must be between 0 and 100."));
        }

        // Ejecutar ambas operaciones en una TRANSACCIÓN con REINTENTOS
        Mono<LikeModel> transactionOperation = transactionsRepository.executeInTransaction(template -> {
            log.debug("Executing like creation and rating update in transaction");

            // 1. Actualizar el rating promedio del usuario receptor
            return updateAverageRatingUseCase.execute(receiverUID, likeValue)
                    .doOnSuccess(updatedUser -> 
                        log.debug("Rating updated for user: {}, new average: {}", 
                                receiverUID, updatedUser.getAverageReactionValue()))
                    
                    // 2. Crear el like
                    .then(createLikeUseCase.execute(userUID, receiverUID, likeValue))
                    .doOnSuccess(createdLike -> 
                        log.debug("Like created with ID: {}", createdLike.getLikeUID()))
                    
                    // Si cualquier operación falla, la transacción hace rollback automático
                    .doOnError(error -> 
                        log.error("Transaction failed for like creation. User: {}, Receiver: {}, Error: {}", 
                                userUID, receiverUID, error.getMessage()));
        });

        // Aplicar reintentos para errores transitorios
        return retryHelper.executeWithRetry(
                transactionOperation, 
                "CreateLike_Transaction_" + userUID + "_to_" + receiverUID
        )
        .doOnSuccess(like -> log.info("Like created successfully: {}", like.getLikeUID()))
        .doOnError(error -> log.error("Failed to create like for user: {} -> {}: {}", 
                userUID, receiverUID, error.getMessage()));
    }

    /**
     * Versión alternativa que acepta parámetros directos en lugar de Map
     * Más type-safe y fácil de usar
     */
    public Mono<LikeModel> execute(String senderUID, String receiverUID, Integer likeValue) {
        Map<String, Object> likeData = Map.of(
            "receiverUID", receiverUID,
            "likeValue", likeValue
        );
        return execute(senderUID, likeData);
    }
}
