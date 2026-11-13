package com.example.chatapp_backend.chat.service

import com.example.chatapp_backend.chat.dto.SendMessageRequest
import com.example.chatapp_backend.persistence.entity.*
import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
import com.example.chatapp_backend.persistence.repository.ChatRoomRepository
import com.example.chatapp_backend.persistence.repository.MessageRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.UUID

class MessageServiceTest {
    private val messages = Mockito.mock(MessageRepository::class.java)
    private val participants = Mockito.mock(ChatParticipantRepository::class.java)
    private val rooms = Mockito.mock(ChatRoomRepository::class.java)
    private val users = Mockito.mock(UserRepository::class.java)
    private val service = MessageService(messages, participants, rooms, users)

    @Test
    fun `message sender is authenticated user and send is persisted`() {
        val senderId = UUID.randomUUID()
        val roomId = UUID.randomUUID()
        val sender = UserEntity(username = "sender", email = "sender@example.test").also { it.id = senderId }
        val room = ChatRoomEntity(roomType = RoomType.GROUP, createdBy = sender).also { it.id = roomId }
        Mockito.`when`(users.findById(senderId)).thenReturn(Optional.of(sender))
        Mockito.`when`(rooms.findById(roomId)).thenReturn(Optional.of(room))
        Mockito.`when`(participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, senderId)).thenReturn(true)
        Mockito.`when`(messages.findBySender_IdAndClientMessageId(senderId, UUID.fromString("93000000-0000-0000-0000-000000000001"))).thenReturn(null)
        Mockito.`when`(messages.saveAndFlush(Mockito.any(MessageEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<MessageEntity>(0).also { it.id = UUID.randomUUID() }
        }

        val result = service.send(senderId, roomId, SendMessageRequest(UUID.fromString("93000000-0000-0000-0000-000000000001"), "hello"))

        assertEquals(senderId, result.senderId)
        assertEquals(roomId, result.roomId)
        assertEquals("hello", result.content)
    }

    @Test
    fun `non member cannot send message`() {
        val senderId = UUID.randomUUID()
        val roomId = UUID.randomUUID()
        val sender = UserEntity(username = "sender", email = "sender@example.test").also { it.id = senderId }
        Mockito.`when`(users.findById(senderId)).thenReturn(Optional.of(sender))
        Mockito.`when`(rooms.findById(roomId)).thenReturn(Optional.of(ChatRoomEntity(roomType = RoomType.GROUP, createdBy = sender)))
        Mockito.`when`(participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, senderId)).thenReturn(false)

        assertThrows(ResponseStatusException::class.java) {
            service.send(senderId, roomId, SendMessageRequest(UUID.randomUUID(), "hello"))
        }
    }

    @Test
    fun `client cannot send system messages`() {
        val senderId = UUID.randomUUID()
        val roomId = UUID.randomUUID()
        val sender = UserEntity(username = "sender", email = "sender@example.test").also { it.id = senderId }
        Mockito.`when`(users.findById(senderId)).thenReturn(Optional.of(sender))
        Mockito.`when`(rooms.findById(roomId)).thenReturn(Optional.of(ChatRoomEntity(roomType = RoomType.GROUP, createdBy = sender)))
        Mockito.`when`(participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, senderId)).thenReturn(true)

        assertThrows(ResponseStatusException::class.java) {
            service.send(senderId, roomId, SendMessageRequest(UUID.randomUUID(), "internal", MessageType.SYSTEM))
        }
    }
}
