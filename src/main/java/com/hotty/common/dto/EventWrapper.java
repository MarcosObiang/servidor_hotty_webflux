package com.hotty.common.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.common.enums.PublishEventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Wrapper común para eventos de publicación en todo el monolito.
 * Usado por todos los servicios para envolver datos en eventos.
 *
 * @param <T> El tipo de datos que contiene el evento
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventWrapper<T> {

    private PublishEventType eventType;
    private T body;
    private String resourceUID;
    private String receiverUID;
    private String dataType;

    // Constructor de conveniencia
    public EventWrapper(PublishEventType eventType, T body, String resourceUID) {
        this.eventType = eventType;
        this.body = body;
        this.resourceUID = resourceUID;
    }

    // Constructor de conveniencia con dataType
    public EventWrapper(PublishEventType eventType, T body, String resourceUID, String dataType) {
        this.eventType = eventType;
        this.body = body;
        this.resourceUID = resourceUID;
        this.dataType = dataType;
    }

    /**
     * Convierte este EventWrapper a JSON string.
     * Útil para WebSocket y otros casos donde se necesita serialización.
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}"; // fallback: cadena JSON vacía
        }
    }
}
