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
            StompCommand.CONNECT -> authenticate(accessor)
            StompCommand.SUBSCRIBE -> authorizeSubscription(accessor)
            StompCommand.SEND -> authorizeSend(accessor)
            else -> Unit
        }
        return message
    }

    private fun authenticate(accessor: StompHeaderAccessor) {
        val authorization = accessor.getFirstNativeHeader("Authorization")
            ?: throw StompSecurityException("Missing STOMP Authorization header")
        if (!authorization.startsWith("Bearer ")) throw StompSecurityException("Authorization must use Bearer token")
        val subject = runCatching { jwtService.parseSubject(authorization.removePrefix("Bearer ").trim()) }
            .getOrElse { throw StompSecurityException("Invalid or expired access token") }
        val user = users.findById(subject).orElseThrow { StompSecurityException("Unknown account") }
        if (!user.isActive) throw StompSecurityException("Account is disabled")
        accessor.user = UsernamePasswordAuthenticationToken(
            AuthenticatedUser(user.id, user.username),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    private fun authorizeSubscription(accessor: StompHeaderAccessor) {
        val principal = authenticatedPrincipal(accessor)
        val destination = accessor.destination ?: throw StompSecurityException("Subscription destination is required")
        if (destination == "/user/queue/errors" || destination == "/user/queue/messages") return
        val roomId = ROOM_MESSAGES_TOPIC.matchEntire(destination)?.groupValues?.get(1)?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: throw StompSecurityException("Subscription destination is not allowed")
        if (!participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, principal.id)) {
            throw StompSecurityException("Active room membership is required to subscribe")
        }
    }

    private fun authorizeSend(accessor: StompHeaderAccessor) {
        authenticatedPrincipal(accessor)
        val destination = accessor.destination
        if (destination !in ALLOWED_SEND_DESTINATIONS) throw StompSecurityException("STOMP send destination is not allowed")
    }

    private fun authenticatedPrincipal(accessor: StompHeaderAccessor): AuthenticatedUser =
        (accessor.user as? org.springframework.security.authentication.UsernamePasswordAuthenticationToken)
            ?.principal as? AuthenticatedUser
            ?: throw StompSecurityException("STOMP connection is not authenticated")

    companion object {
        private val ROOM_MESSAGES_TOPIC = Regex("^/topic/rooms/([0-9a-fA-F-]{36})/messages$")
        private val ALLOWED_SEND_DESTINATIONS = setOf("/app/chat.send")
    }
}

class StompSecurityException(message: String) : RuntimeException(message)
