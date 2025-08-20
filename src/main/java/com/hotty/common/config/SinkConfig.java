package com.hotty.common.config;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import com.hotty.realtime_service.components.RealtimeHandler;
import com.hotty.common.dto.EventWrapper;

import reactor.core.publisher.Sinks;

@Configuration
public class SinkConfig {
    @Bean
    public Sinks.Many<EventWrapper<Map<String, Object>>> sink() {
        return Sinks.many().multicast().directBestEffort();
    }

    @Bean
    public WebSocketHandler webSocketHandler(Sinks.Many<EventWrapper<Map<String, Object>>> sink) {
        return new RealtimeHandler(sink);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

  

    @Bean
    public HandlerMapping webSocketMapping(WebSocketHandler handler) {
        Map<String, WebSocketHandler> map = new HashMap<>();

        map.put("/ws/updates", handler); // aquí defines la ruta WebSocket

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();

        mapping.setOrder(-1); // prioridad alta
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
