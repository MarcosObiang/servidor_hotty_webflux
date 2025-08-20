package com.hotty.realtime_service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.reactive.socket.WebSocketSession;

public class WebSocketSessionManager {

      private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // Guardar sesión al conectar
    public static void addSession(String userUID, WebSocketSession session) {
        userSessions.put(userUID, session);
    }

    // Eliminar sesión al desconectar
    public static void removeSession(String userUID) {
        userSessions.remove(userUID);
    }

    // Obtener sesión para enviar mensajes
    public static WebSocketSession getSession(String userUID) {
        return userSessions.get(userUID);
    }

    // Obtener todas las sesiones activas
    public static Map<String, WebSocketSession> getAllSessions() {
        return userSessions;
    }
    
}
