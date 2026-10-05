package com.helpdeskAi.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main STOMP endpoint; SockJS fallback for non-WS browsers
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for topic / queue subscriptions
        registry.enableSimpleBroker("/topic", "/queue");
        // Client sends to /app/... → handled by @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // User-specific destinations
        registry.setUserDestinationPrefix("/user");
    }
}
