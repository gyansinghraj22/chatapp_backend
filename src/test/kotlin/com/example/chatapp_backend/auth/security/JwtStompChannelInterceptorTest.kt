package com.example.chatapp_backend.auth.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import java.util.UUID

class JwtStompChannelInterceptorTest {
    private val jwtService = Mockito.mock(JwtService::class.java)
    private val users = Mockito.mock(com.example.chatapp_backend.persistence.repository.UserRepository::class.java)
    private val participants = Mockito.mock(com.example.chatapp_backend.persistence.repository.ChatParticipantRepository::class.java)
    private val interceptor = JwtStompChannelInterceptor(jwtService, users, participants)

    @Test
    fun `non room subscriptions are rejected`() {
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/topic/rooms/not-a-uuid/messages"
        accessor.user = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            AuthenticatedUser(UUID.randomUUID(), "test"), null, emptyList()
        )
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        assertFalse(interceptor.preSend(message, Mockito.mock(MessageChannel::class.java)) != null)
    }

    @Test
    fun `room topic membership is checked`() {
        val userId = UUID.randomUUID()
        val roomId = UUID.randomUUID()
        Mockito.`when`(participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, userId)).thenReturn(true)
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/topic/rooms/$roomId/messages"
        accessor.user = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            AuthenticatedUser(userId, "test"), null, emptyList()
        )
        val message: Message<ByteArray> = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        assertTrue(interceptor.preSend(message, Mockito.mock(MessageChannel::class.java)) != null)
    }
}
