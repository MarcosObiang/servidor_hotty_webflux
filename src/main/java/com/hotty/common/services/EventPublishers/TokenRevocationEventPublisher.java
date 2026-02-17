package com.hotty.common.services.EventPublishers;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.auth_service.models.AuthTokenDataModel;
import com.hotty.common.dto.EventWrapper;
import com.hotty.common.enums.PublishEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

/**
 * Publisher para notificar eventos de revocación de tokens al servicio de tiempo real.
 * 
 * Este publisher notifica cuando:
 * - Se refresca un token (el access token anterior se revoca)
 * - Se cierra una sesión (ambos tokens se revocan)
 * - Se revoca manualmente un token por seguridad
 * 
 * El servicio de tiempo real escucha estos eventos para:
 * - Cerrar conexiones WebSocket asociadas al token revocado
 * - Limpiar sesiones activas
 * - Mantener coherencia en el estado de autenticación
 */
@Component
public class TokenRevocationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationEventPublisher.class);

    // Canal único para todos los eventos de usuario (consistente con otros publishers)
    private static final String USER_EVENTS_CHANNEL = "user:events";
    private static final String TOKEN_DATA_TYPE = "token_revocation";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public TokenRevocationEventPublisher(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica evento cuando se revoca un access token (ej: durante refresh).
     * 
     * @param tokenData El modelo de token que contiene el access token revocado
     * @return Mono<Void> que se completa cuando el evento se publica
     */
    public Mono<Void> publishAccessTokenRevoked(AuthTokenDataModel tokenData) {
        TokenRevocationEventData revocationData = new TokenRevocationEventData();
        revocationData.setTokenUID(tokenData.getTokenUID());
        revocationData.setUserUID(tokenData.getUserUID());
        revocationData.setAccessToken(tokenData.getToken());
        revocationData.setRevocationType("ACCESS_TOKEN_REFRESH");
        revocationData.setReason("Token refreshed - previous access token revoked");

        EventWrapper<TokenRevocationEventData> event = new EventWrapper<>();
        event.setEventType(PublishEventType.DELETE);
        event.setBody(revocationData);
        event.setResourceUID(tokenData.getTokenUID());
        event.setReceiverUID(tokenData.getUserUID());
        event.setDataType(TOKEN_DATA_TYPE);
        
        return publish(event, "ACCESS TOKEN REVOKED (REFRESH)");
    }

    /**
     * Publica evento cuando se revoca una sesión completa (logout).
     * 
     * @param tokenData El modelo de token que contiene ambos tokens revocados
     * @return Mono<Void> que se completa cuando el evento se publica
     */
    public Mono<Void> publishSessionRevoked(AuthTokenDataModel tokenData) {
        TokenRevocationEventData revocationData = new TokenRevocationEventData();
        revocationData.setTokenUID(tokenData.getTokenUID());
        revocationData.setUserUID(tokenData.getUserUID());
        revocationData.setAccessToken(tokenData.getToken());
        revocationData.setRefreshToken(tokenData.getRefreshToken());
        revocationData.setRevocationType("SESSION_LOGOUT");
        revocationData.setReason("User logout - complete session revoked");

        EventWrapper<TokenRevocationEventData> event = new EventWrapper<>();
        event.setEventType(PublishEventType.DELETE);
        event.setBody(revocationData);
        event.setResourceUID(tokenData.getTokenUID());
        event.setReceiverUID(tokenData.getUserUID());
        event.setDataType(TOKEN_DATA_TYPE);
        
        return publish(event, "SESSION REVOKED (LOGOUT)");
    }

    /**
     * Publica evento cuando se revoca un token por motivos de seguridad.
     * 
     * @param tokenData El modelo de token revocado
     * @param reason Razón específica de la revocación
     * @return Mono<Void> que se completa cuando el evento se publica
     */
    public Mono<Void> publishTokenRevokedForSecurity(AuthTokenDataModel tokenData, String reason) {
        TokenRevocationEventData revocationData = new TokenRevocationEventData();
        revocationData.setTokenUID(tokenData.getTokenUID());
        revocationData.setUserUID(tokenData.getUserUID());
        revocationData.setAccessToken(tokenData.getToken());
        revocationData.setRefreshToken(tokenData.getRefreshToken());
        revocationData.setRevocationType("SECURITY_REVOCATION");
        revocationData.setReason(reason);

        EventWrapper<TokenRevocationEventData> event = new EventWrapper<>();
        event.setEventType(PublishEventType.DELETE);
        event.setBody(revocationData);
        event.setResourceUID(tokenData.getTokenUID());
        event.setReceiverUID(tokenData.getUserUID());
        event.setDataType(TOKEN_DATA_TYPE);
        
        return publish(event, "TOKEN REVOKED (SECURITY)");
    }

    /**
     * Método privado para publicar eventos con logging específico.
     */
    private Mono<Void> publish(EventWrapper<TokenRevocationEventData> event, String logContext) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(eventJson -> reactiveRedisTemplate.convertAndSend(USER_EVENTS_CHANNEL, eventJson))
                .doOnSuccess(clients -> log.info(
                        "{} - Token revocation event published for tokenUID '{}', userUID '{}'. Notified {} subscribers.",
                        logContext, event.getResourceUID(), event.getReceiverUID(), clients))
                .doOnError(e -> log.error("Error publishing token revocation event: {}", event, e))
                .then();
    }

    /**
     * Clase interna para los datos específicos del evento de revocación.
     * 
     * Esta clase contiene toda la información necesaria para que el servicio
     * de tiempo real pueda identificar y cerrar las sesiones apropiadas.
     */
    public static class TokenRevocationEventData {
        private String tokenUID;
        private String userUID;
        private String accessToken;
        private String refreshToken;
        private String revocationType; // ACCESS_TOKEN_REFRESH, SESSION_LOGOUT, SECURITY_REVOCATION
        private String reason;

        // Getters y Setters
        public String getTokenUID() { return tokenUID; }
        public void setTokenUID(String tokenUID) { this.tokenUID = tokenUID; }

        public String getUserUID() { return userUID; }
        public void setUserUID(String userUID) { this.userUID = userUID; }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public String getRevocationType() { return revocationType; }
        public void setRevocationType(String revocationType) { this.revocationType = revocationType; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        @Override
        public String toString() {
            return "TokenRevocationEventData{" +
                    "tokenUID='" + tokenUID + '\'' +
                    ", userUID='" + userUID + '\'' +
                    ", revocationType='" + revocationType + '\'' +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}
