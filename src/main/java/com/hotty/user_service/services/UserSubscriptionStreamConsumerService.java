package com.hotty.user_service.services;

import com.hotty.common.services.EventPublishers.UserEventPublisherService;
import com.hotty.user_service.DTOs.UserSubscriptionUpdateDTO;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserSubscription;
import com.hotty.user_service.repository.interfaces.UserModelRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;

/**
 * Servicio que consume el stream de actualizaciones de suscripciones
 * Procesa las actualizaciones de forma reactiva y no bloqueante
 */
@Service
public class UserSubscriptionStreamConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionStreamConsumerService.class);

    private final Flux<UserSubscriptionUpdateDTO> subscriptionUpdatesStream;
    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService userEventPublisherService;

    public UserSubscriptionStreamConsumerService(
            Flux<UserSubscriptionUpdateDTO> subscriptionUpdatesStream,
            UserModelRepository userModelRepository,
            UserEventPublisherService userEventPublisherService) {
        this.subscriptionUpdatesStream = subscriptionUpdatesStream;
        this.userModelRepository = userModelRepository;
        this.userEventPublisherService = userEventPublisherService;
        logger.info("✅ UserSubscriptionStreamConsumerService initialized with shared stream");
    }

    /**
     * Inicia el consumo del stream de forma reactiva y no bloqueante
     */
    @PostConstruct
    public void startStreamConsumption() {
        logger.info("🚀 Iniciando consumo reactivo del stream de actualizaciones de suscripciones...");

        subscriptionUpdatesStream
                // Procesar cada actualización de forma no bloqueante
                .flatMap(this::handleSubscriptionUpdateReactive)
                // Ejecutar en scheduler separado para operaciones de BD
                .subscribeOn(Schedulers.boundedElastic())
                // Manejo de errores
                .doOnError(error -> logger.error("❌ Error en el stream de suscripciones: {}", error.getMessage()))
                .onErrorContinue((error, item) -> {
                    logger.error("❌ Error procesando actualización de suscripción para item {}: {}",
                            item, error.getMessage());
                })
                .doOnComplete(() -> logger.info("✅ Stream de suscripciones completado"))
                .subscribe();

        logger.info("📡 Suscripción reactiva al stream de actualizaciones iniciada correctamente");
    }

    /**
     * Maneja cada actualización de suscripción de forma reactiva
     * Retorna un Mono para mantener el flujo reactivo
     */
    private Mono<Void> handleSubscriptionUpdateReactive(UserSubscriptionUpdateDTO updateDTO) {
        logger.info("📥 Procesando actualización de suscripción reactiva:");
        logger.info("   👤 Usuario: {}", updateDTO.getUserUID());
        logger.info("   🎯 Evento: {}", updateDTO.getEventType());
        logger.info("   💎 Premium: {}", updateDTO.getIsUserPremium());
        logger.info("   📊 Estado: {}", updateDTO.getSubscriptionStatus());
        logger.info("   📅 Id del producto: {}", updateDTO.getCurrentProductId());

        // Procesar la actualización de forma reactiva
        return updateUserSubscriptionReactive(updateDTO)
                .doOnSuccess(updatedUser -> logger.info("✅ Usuario actualizado exitosamente: {} - Premium: {}",
                        updatedUser.getUserUID(), updatedUser.getSubscription().getIsUserPremium()))
                // ✅ SOLUCIÓN: Encadenar correctamente la publicación del evento
                .flatMap(updatedUser -> {
                    // Publicar evento y retornar el usuario para continuar la cadena
                    return userEventPublisherService.publishUserUpdated(updatedUser)
                            .doOnSuccess(v -> logger.info("📡 Evento de usuario actualizado publicado para: {}",
                                    updatedUser.getUserUID()))
                            .doOnError(error -> logger.warn("⚠️ Error publicando evento para usuario {}: {}",
                                    updatedUser.getUserUID(), error.getMessage()))
                            .onErrorResume(error -> {
                                // Si falla la publicación del evento, no fallar todo el proceso
                                logger.warn("⚠️ Continuando sin publicar evento para usuario: {}",
                                        updatedUser.getUserUID());
                                return Mono.empty();
                            });
                })
                .doOnError(error -> logger.error("❌ Error actualizando suscripción para usuario {}: {}",
                        updateDTO.getUserUID(), error.getMessage()))
                .then(); // Convertir a Mono<Void> para el stream
    }

    /**
     * Actualiza la suscripción del usuario de forma reactiva
     * Devuelve el UserDataModel actualizado
     */
    private Mono<UserDataModel> updateUserSubscriptionReactive(UserSubscriptionUpdateDTO updateDTO) {
        // Convertir DTO a UserSubscription
        String eventType = updateDTO.getEventType();

        UserSubscription userSubscription = UserSubscription.fromDTO(updateDTO);

        if (eventType.equals("NON_RENEWING_PURCHASE")) {

            String productUID = updateDTO.getCurrentProductId();

            final Integer creditsToAdd;
            switch (productUID) {
                case "fast_recharge_0":
                    creditsToAdd = 600;
                    break;
                case "fast_recharge_1":
                    creditsToAdd = 1200;
                    break;
                case "fast_recharge_2":
                    creditsToAdd = 2000;
                    break;
                default:
                    creditsToAdd = 0;
            }

            return userModelRepository.addCreditsToUser(
                    updateDTO.getAppUserId(),
                    creditsToAdd).flatMap(updatedUser -> {
                        logger.info("✅ Added {} credits to user {} for NON_RENEWING_PURCHASE event",
                                creditsToAdd, updateDTO.getAppUserId());
                        return Mono.just(updatedUser);
                    });
        }

        // Llamar al repository que devuelve Mono<UserDataModel> y aplicar el scheduler
        return userModelRepository.updateUserSubscriptionData(
                updateDTO.getAppUserId(),
                userSubscription);
        // ✅ Ya no usamos .then() aquí para mantener el UserDataModel
    }

    /**
     * Método público para obtener el stream (por si otros servicios lo necesitan)
     */
    public Flux<UserSubscriptionUpdateDTO> getSubscriptionUpdates() {
        return subscriptionUpdatesStream;
    }
}
