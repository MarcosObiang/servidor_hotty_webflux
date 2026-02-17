package com.hotty.common.common_usecases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hotty.likes_service.usecases.GetLikeUseCase;
import com.hotty.likes_service.usecases.DeleteLikeUseCase;
import com.hotty.chat_service.usecases.chat.CreateChatUseCase;
import com.hotty.user_service.usecases.GetUserByUIDUseCase;
import com.hotty.common.common_transactions.MongoTransactionsRepository;
import com.hotty.common.common_transactions.TransactionRetryHelper;
import com.hotty.likes_service.model.LikeModel;
import com.hotty.user_service.model.UserDataModel;

import reactor.core.publisher.Mono;

@Component
public class AcceptLikeUseCase {

    private static final Logger log = LoggerFactory.getLogger(AcceptLikeUseCase.class);

    private final MongoTransactionsRepository transactionsRepository;
    private final TransactionRetryHelper retryHelper;
    private final GetLikeUseCase getLikeUseCase;
    private final DeleteLikeUseCase deleteLikeUseCase;
    private final CreateChatUseCase createChatUseCase;
    private final GetUserByUIDUseCase getUserByUIDUseCase;

    public AcceptLikeUseCase(MongoTransactionsRepository transactionsRepository,
                           TransactionRetryHelper retryHelper,
                           GetLikeUseCase getLikeUseCase,
                           DeleteLikeUseCase deleteLikeUseCase,
                           CreateChatUseCase createChatUseCase,
                           GetUserByUIDUseCase getUserByUIDUseCase) {
        this.transactionsRepository = transactionsRepository;
        this.retryHelper = retryHelper;
        this.getLikeUseCase = getLikeUseCase;
        this.deleteLikeUseCase = deleteLikeUseCase;
        this.createChatUseCase = createChatUseCase;
        this.getUserByUIDUseCase = getUserByUIDUseCase;
    }

    public Mono<String> execute(String likeUID, String userUID) {
        log.info("Executing AcceptLike for likeUID: {} by userUID: {}", likeUID, userUID);

        // 1. Operaciones de validación (fuera de transacción)
        return getLikeUseCase.execute(likeUID)
                .flatMap(like -> validateAndGetUsers(like, userUID))
                
                // 2. Operaciones críticas EN TRANSACCIÓN con REINTENTOS
                .flatMap(userData -> {
                    Mono<String> transactionOperation = transactionsRepository.executeInTransaction(template -> {
                        // Crear chat Y eliminar like en UNA SOLA transacción
                        return createChatUseCase.execute(
                                userData.sender.getUserUID(),
                                userData.sender.getName(),
                                userData.sender.getUserImage1(),
                                userData.receiver.getUserUID(),
                                userData.receiver.getName(),
                                userData.receiver.getUserImage1())
                            .flatMap(chatModel -> 
                                deleteLikeUseCase.execute(userUID, likeUID)
                                    .thenReturn("Like accepted and chat created successfully")
                            );
                    });
                    
                    // Aplicar reintentos para errores transitorios
                    return retryHelper.executeWithRetry(
                        transactionOperation, 
                        "AcceptLike_Transaction_" + likeUID
                    );
                });
    }
    
    private Mono<UserData> validateAndGetUsers(LikeModel like, String userUID) {
        if (!like.getReceiverUID().equals(userUID)) {
            return Mono.error(new IllegalArgumentException(
                "User " + userUID + " is not authorized to accept this like"));
        }

        return Mono.zip(
                getUserByUIDUseCase.execute(like.getSenderUID()),
                getUserByUIDUseCase.execute(like.getReceiverUID()))
            .map(tuple -> new UserData(like, tuple.getT1(), tuple.getT2()));
    }

    // Clase auxiliar para encapsular los datos
    private static class UserData {
        final UserDataModel sender;
        final UserDataModel receiver;

        UserData(LikeModel like, UserDataModel sender, UserDataModel receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }
    }
}
