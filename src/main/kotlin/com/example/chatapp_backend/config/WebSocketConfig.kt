package com.example.chatapp_backend.config

import com.example.chatapp_backend.auth.security.JwtStompChannelInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.time.Duration

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtStompChannelInterceptor: JwtStompChannelInterceptor,
    @Value("\${app.websocket.allowed-origins}") private val allowedOrigins: String
) : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(longArrayOf(Duration.ofSeconds(15).toMillis(), Duration.ofSeconds(15).toMillis()))
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(*allowedOrigins.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray())
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(jwtStompChannelInterceptor)
    }

    override fun configureWebSocketTransport(registry: org.springframework.web.socket.config.annotation.WebSocketTransportRegistration) {
        registry.setMessageSizeLimit(64 * 1024)
            .setSendBufferSizeLimit(512 * 1024)
            .setSendTimeLimit(15_000)
    }
}
