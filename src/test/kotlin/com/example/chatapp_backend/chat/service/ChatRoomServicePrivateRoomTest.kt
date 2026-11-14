package com.example.chatapp_backend.chat.service

import com.example.chatapp_backend.chat.dto.CreatePrivateRoomRequest
import com.example.chatapp_backend.persistence.entity.ChatRoomEntity
import com.example.chatapp_backend.persistence.entity.RoomType
import com.example.chatapp_backend.persistence.entity.UserEntity
import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
import com.example.chatapp_backend.persistence.repository.ChatRoomRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.UUID

class ChatRoomServicePrivateRoomTest {
    private val rooms = Mockito.mock(ChatRoomRepository::class.java)
    private val participants = Mockito.mock(ChatParticipantRepository::class.java)
    private val users = Mockito.mock(UserRepository::class.java)
    private val service = ChatRoomService(rooms, participants, users)

    @Test
    fun `private room includes exactly authenticated user and requested peer`() {
        val actorId = UUID.randomUUID(); val peerId = UUID.randomUUID()
        val actor = UserEntity(username = "actor", email = "actor@example.test").also { it.id = actorId }
        val peer = UserEntity(username = "peer", email = "peer@example.test").also { it.id = peerId }
        val roomId = UUID.randomUUID()
        val room = ChatRoomEntity(roomType = RoomType.PRIVATE, createdBy = actor,
            directChatKey = listOf(actorId, peerId).map(UUID::toString).sorted().joinToString(":" )).also { it.id = roomId }
        Mockito.`when`(users.findById(actorId)).thenReturn(Optional.of(actor))
        Mockito.`when`(users.findAllById(Mockito.anyCollection())).thenReturn(listOf(actor, peer))
        Mockito.`when`(rooms.findByDirectChatKey(room.directChatKey!!)).thenReturn(room)
        Mockito.`when`(participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId)).thenReturn(
            listOf(
                com.example.chatapp_backend.persistence.entity.ChatParticipantEntity(room = room, user = actor),
                com.example.chatapp_backend.persistence.entity.ChatParticipantEntity(room = room, user = peer)
            )
        )

        val response = service.createPrivateRoom(actorId, CreatePrivateRoomRequest(setOf(peerId)))
        assertEquals(2, response.participants.size)
        assertEquals(RoomType.PRIVATE, response.type)
    }

    @Test
    fun `private room rejects self conversation`() {
        val actorId = UUID.randomUUID()
        val actor = UserEntity(username = "actor", email = "actor@example.test").also { it.id = actorId }
        Mockito.`when`(users.findById(actorId)).thenReturn(Optional.of(actor))
        assertThrows(ResponseStatusException::class.java) {
            service.createPrivateRoom(actorId, CreatePrivateRoomRequest(setOf(actorId)))
        }
    }
}
