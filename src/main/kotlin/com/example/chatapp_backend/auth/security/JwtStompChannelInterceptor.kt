package com.example.chatapp_backend.auth.security

import com.example.chatapp_backend.persistence.repository.UserRepository
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class JwtStompChannelInterceptor(
    private val jwtService: JwtService,
    private val users: UserRepository
) : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
        when (accessor.command) {
            StompCommand.CONNECT -> {
                val bearer = accessor.getFirstNativeHeader("Authorization")
                    ?: throw IllegalArgumentException("Missing STOMP Authorization header")
                require(bearer.startsWith("Bearer ")) { "Authorization must use Bearer token" }
                val subject = jwtService.parseSubject(bearer.removePrefix("Bearer ").trim())
                val user = users.findById(subject).orElseThrow { IllegalArgumentException("Unknown user") }
                require(user.isActive) { "Account is disabled" }
                accessor.user = UsernamePasswordAuthenticationToken(
                    AuthenticatedUser(user.id, user.username),
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )
            }
            StompCommand.SEND, StompCommand.SUBSCRIBE ->
                requireNotNull(accessor.user) { "STOMP connection is not authenticated" }
            else -> Unit
        }
        return message
    }
}
