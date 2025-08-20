package com.hotty.common.common_transactions;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.ReactiveTransactionManager;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.List;

@Repository
public class MongoTransactionsRepository {
    
    private final ReactiveMongoTemplate mongoTemplate;
    private final TransactionalOperator transactionalOperator;
    
    public MongoTransactionsRepository(ReactiveMongoTemplate mongoTemplate, 
                                     ReactiveTransactionManager transactionManager) {
        this.mongoTemplate = mongoTemplate;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
    }
    
    /**
     * Ejecuta múltiples operaciones en una sola transacción
     * @param operations Función que contiene todas las operaciones a ejecutar
     * @return Mono con el resultado de las operaciones
     */
    public <T> Mono<T> executeInTransaction(Function<ReactiveMongoTemplate, Mono<T>> operations) {
        return operations.apply(mongoTemplate)
                .as(transactionalOperator::transactional);
    }
    
    /**
     * Ejecuta múltiples operaciones en una sola transacción con rollback manual
     * @param operations Lista de operaciones a ejecutar
     * @return Mono con el resultado
     */
    public <T> Mono<T> executeMultipleOperations(List<Function<ReactiveMongoTemplate, Mono<?>>> operations, 
                                                T finalResult) {
        return operations.stream()
                .reduce(Mono.just(mongoTemplate),
                        (mono, operation) -> mono.flatMap(template -> 
                            operation.apply(template).thenReturn(template)),
                        (m1, m2) -> m1)
                .thenReturn(finalResult)
                .as(transactionalOperator::transactional);
    }
    
    /**
     * Inserta un documento en transacción
     */
    public <T> Mono<T> insertInTransaction(T document) {
        return mongoTemplate.insert(document)
                .as(transactionalOperator::transactional);
    }
    
    /**
     * Actualiza un documento en transacción
     */
    public <T> Mono<T> updateInTransaction(Query query, Update update, Class<T> entityClass) {
        return mongoTemplate.findAndModify(query, update, entityClass)
                .as(transactionalOperator::transactional);
    }
    
    /**
     * Elimina un documento en transacción
     */
    public <T> Mono<T> deleteInTransaction(Query query, Class<T> entityClass) {
        return mongoTemplate.findAndRemove(query, entityClass)
                .as(transactionalOperator::transactional);
    }
    
    /**
     * Ejecuta una secuencia de operaciones de forma encadenada en transacción
     * Útil para casos como: crear chat -> eliminar like -> actualizar usuario
     */
    public <T> Mono<T> executeSequence(Mono<T> sequence) {
        return sequence.as(transactionalOperator::transactional);
    }
    
    /**
     * Método específico para operaciones complejas como AcceptLike
     * Permite pasar múltiples pasos y ejecutarlos en una sola transacción
     */
    public <T> Mono<T> executeComplexOperation(
            Function<ReactiveMongoTemplate, Mono<T>> step1,
            Function<T, Function<ReactiveMongoTemplate, Mono<T>>> step2,
            Function<T, Function<ReactiveMongoTemplate, Mono<T>>> step3) {
        
        return step1.apply(mongoTemplate)
                .flatMap(result1 -> 
                    step2.apply(result1).apply(mongoTemplate)
                        .flatMap(result2 -> 
                            step3.apply(result2).apply(mongoTemplate)))
                .as(transactionalOperator::transactional);
    }
    
    /**
     * Rollback manual con cleanup
     * Útil cuando necesitas hacer cleanup específico antes del rollback
     */
    public <T> Mono<T> executeWithManualRollback(
            Function<ReactiveMongoTemplate, Mono<T>> operations,
            Function<ReactiveMongoTemplate, Mono<Void>> rollbackOperations) {
        
        return operations.apply(mongoTemplate)
                .onErrorResume(error -> 
                    rollbackOperations.apply(mongoTemplate)
                        .then(Mono.error(error)))
                .as(transactionalOperator::transactional);
    }
}
