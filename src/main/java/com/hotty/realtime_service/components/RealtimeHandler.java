package com.hotty.realtime_service.components;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.hotty.realtime_service.WebSocketSessionManager;
import com.hotty.common.dto.EventWrapper;

import io.micrometer.common.lang.NonNull;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class RealtimeHandler implements WebSocketHandler {

    private final Sinks.Many<EventWrapper<Map<String, Object>>> sink;

    public RealtimeHandler(Sinks.Many<EventWrapper<Map<String, Object>>> sink) {
        this.sink = sink;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull WebSocketSession session) {
        // Obtenemos el userUID de las cabeceras de la conexión WebSocket.
        final String userUID = getUserUId(session);

        // Si no hay userUID, no podemos asociar la sesión a un usuario, así que la cerramos.
        if (userUID.isBlank()) {
            return session.close(new org.springframework.web.reactive.socket.CloseStatus(4001, "userUID header is missing or empty"));
        }

        // Añadimos la sesión al manager para poder encontrarla más tarde.
        WebSocketSessionManager.addSession(userUID, session);
        System.out.println("Usuario conectado: " + userUID + " con session ID: " + session.getId());

        // Flujo de salida: Escucha el sink, filtra los mensajes para este usuario y los envía.
        Mono<Void> output = session.send(
            sink.asFlux()
                // Filtramos los eventos. Un evento es para esta sesión si:
                // 1. El receiverUID del evento coincide con el userUID de esta sesión.
                // 2. El receiverUID es nulo o vacío (lo consideramos un broadcast para todos).
                .filter(event -> {
                    String receiverUID = event.getReceiverUID();
                    return receiverUID == null || receiverUID.isBlank() || receiverUID.equals(userUID);
                })
                // Mapeamos el evento a un mensaje de texto WebSocket usando el método toJson().
                .map(event -> session.textMessage(event.toJson()))
        ).doFinally(signalType -> {
            // Este bloque se ejecuta cuando la conexión se cierra (por cualquier motivo).
            // Es crucial limpiar la sesión del manager para no intentar enviar mensajes a sesiones cerradas.
            WebSocketSessionManager.removeSession(userUID);
            System.out.println("Usuario desconectado: " + userUID + " (Signal: " + signalType + ")");
        });

        // Flujo de entrada: Escucha los mensajes que envía el cliente.
        // Por ahora, solo los imprimimos en la consola.
        Mono<Void> input = session.receive()
                .doOnNext(message -> {
                    System.out.println("Mensaje recibido del cliente " + userUID + ": " + message.getPayloadAsText());
                    // Aquí se podría añadir lógica para procesar comandos del cliente.
                })
                .then();

        // Mono.zip combina la entrada y la salida. La conexión se mantiene abierta
        // mientras ambos flujos estén activos.
        return Mono.zip(input, output).then();
    }

    private String getUserUId(WebSocketSession session) {
        // Usamos Objects.requireNonNullElse para obtener el header y devolver "" si es nulo,
        // evitando NullPointerExceptions.
        return Objects.requireNonNullElse(session.getHandshakeInfo().getHeaders().getFirst("userUID"), "");
    }
}
