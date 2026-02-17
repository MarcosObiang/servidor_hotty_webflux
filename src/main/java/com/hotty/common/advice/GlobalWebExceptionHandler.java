package com.hotty.common.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotty.ApiResponse.ApiResponse;
import com.hotty.gateway_server.exceptions.UnauthorizedException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof UnauthorizedException) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            ApiResponse<Void> body = ApiResponse.error("UNAUTHORIZED", ex.getMessage());
            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsBytes(body);
            } catch (Exception e) {
                bytes = ("{\"error\":\"UNAUTHORIZED\",\"message\":\"" + ex.getMessage() + "\"}").getBytes();
            }
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory().wrap(bytes)));
        }
        // Otros errores...
        return Mono.error(ex);
    }
}