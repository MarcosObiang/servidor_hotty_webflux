package com.hotty.realtime_service.model;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RealTimeWebSocketMessageContainer {
    private Map<String, Object> data;
    private String dataType;
    private String eventType;
    private String receiverUID;

    public RealTimeWebSocketMessageContainer(Map<String, Object> data, String dataType, String eventType,
            String receiverUID) {
        this.data = data;
        this.dataType = dataType;
        this.eventType = eventType;
        this.receiverUID = receiverUID;
    }

    public RealTimeWebSocketMessageContainer() {
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getReceiverUID() {
        return receiverUID;
    }

    public void setReceiverUID(String receiverUID) {
        this.receiverUID = receiverUID;
    }

    @Override
    public String toString() {
        return "RealTimeWebSocketMessageContainer{" +
                "data='" + data + '\'' +
                ", dataType='" + dataType + '\'' +
                ", eventType='" + eventType + '\'' +
                ", receiverUID='" + receiverUID + '\'' +
                '}';
    }

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
