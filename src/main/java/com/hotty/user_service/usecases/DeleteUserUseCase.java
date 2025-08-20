package com.hotty.user_service.usecases;

import org.springframework.stereotype.Component;

import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.services.UserEventPublisherService;

import reactor.core.publisher.Mono;

@Component
public class DeleteUserUseCase {

    private final UserModelRepository userModelRepository;
    private final UserEventPublisherService eventPublisher;

    public DeleteUserUseCase(UserModelRepository userModelRepository, UserEventPublisherService eventPublisher) {
        this.userModelRepository = userModelRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Ejecuta la lógica de negocio para eliminar un usuario.
     *
     * @param userUID El UID del usuario a eliminar.
     * @return Un Mono<Void> que se completa cuando el usuario ha sido eliminado.
     */
    public Mono<Void> execute(String userUID) {
        // Después de que se complete la eliminación en la BD, publica el evento.
        return userModelRepository.deleteByUserUID(userUID)
                .then(eventPublisher.publishUserDeleted(userUID));
    }
}
