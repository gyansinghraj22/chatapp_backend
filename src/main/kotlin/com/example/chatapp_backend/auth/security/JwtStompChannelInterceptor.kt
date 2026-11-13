package com.example.chatapp_backend.auth.security

import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
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
import java.security.Principal
import java.util.UUID

@Component
class JwtStompChannelInterceptor(
    private val jwtService: JwtService,
    private val users: UserRepository,
    private val participants: ChatParticipantRepository
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
            StompCommand.SUBSCRIBE -> {
                val principal = accessor.user?.principal as? AuthenticatedUser
                    ?: throw IllegalArgumentException("STOMP connection is not authenticated")
                val destination = accessor.destination ?: throw IllegalArgumentException("STOMP destination is required")
                if (destination == "/user/queue/messages" || destination == "/user/queue/errors") return message
                val roomId = Regex("^/topic/rooms/([0-9a-fA-F-]{36})/messages$")
                    .matchEntire(destination)?.groupValues?.get(1)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: throw IllegalArgumentException("Subscription destination is not allowed")
                if (!participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, principal.id)) {
                    throw IllegalArgumentException("Active room membership is required to subscribe")
                }
            }
            StompCommand.SEND -> {
                requireNotNull(accessor.user) { "STOMP connection is not authenticated" }
                val destination = accessor.destination ?: throw IllegalArgumentException("STOMP destination is required")
                require(destination == "/app/chat.send") { "STOMP destination is not allowed" }
            }
            else -> Unit
        }
        return message
    }
}
