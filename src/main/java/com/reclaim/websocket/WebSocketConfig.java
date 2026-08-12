package com.reclaim.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the live-messaging WebSocket endpoint at /ws.
 * Allowed origins mirror the REST CORS config (CORS_ORIGINS env var).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageSocketHandler handler;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public WebSocketConfig(MessageSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
