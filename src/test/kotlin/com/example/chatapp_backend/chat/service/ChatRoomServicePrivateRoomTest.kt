package com.example.chatapp_backend.chat.service

import com.example.chatapp_backend.chat.dto.CreatePrivateRoomRequest
import com.example.chatapp_backend.persistence.entity.ChatParticipantEntity
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
    fun `private room includes the authenticated user and exactly one peer`() {
        val actorId = UUID.randomUUID()
        val peerId = UUID.randomUUID()
        val actor = UserEntity(username = "actor", email = "actor@example.test").also { it.id = actorId }
        val peer = UserEntity(username = "peer", email = "peer@example.test").also { it.id = peerId }
        val roomId = UUID.randomUUID()
        val directKey = listOf(actorId, peerId).map(UUID::toString).sorted().joinToString(":")
        val room = ChatRoomEntity(roomType = RoomType.PRIVATE, createdBy = actor, directChatKey = directKey).also { it.id = roomId }
        Mockito.`when`(users.findById(actorId)).thenReturn(Optional.of(actor))
        Mockito.`when`(users.findAllById(Mockito.anyCollection())).thenReturn(listOf(actor, peer))
        Mockito.`when`(rooms.findByDirectChatKey(directKey)).thenReturn(room)
        Mockito.`when`(participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId)).thenReturn(
            listOf(ChatParticipantEntity(room = room, user = actor), ChatParticipantEntity(room = room, user = peer))
        )

        val result = service.createPrivateRoom(actorId, CreatePrivateRoomRequest(setOf(peerId)))
        assertEquals(RoomType.PRIVATE, result.type)
        assertEquals(2, result.participants.size)
        Mockito.verify(rooms).insertPrivateRoomIfAbsent(Mockito.any(UUID::class.java), Mockito.eq(actorId), Mockito.eq(directKey))
        Mockito.verify(participants, Mockito.times(2)).insertActiveMemberIfAbsent(Mockito.any(UUID::class.java), Mockito.eq(roomId), Mockito.any(UUID::class.java))
    }

    @Test
    fun `private room rejects self and multi user requests`() {
        val actorId = UUID.randomUUID()
        val actor = UserEntity(username = "actor", email = "actor@example.test").also { it.id = actorId }
        Mockito.`when`(users.findById(actorId)).thenReturn(Optional.of(actor))
        assertThrows(ResponseStatusException::class.java) {
            service.createPrivateRoom(actorId, CreatePrivateRoomRequest(setOf(actorId)))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.createPrivateRoom(actorId, CreatePrivateRoomRequest(setOf(UUID.randomUUID(), UUID.randomUUID())))
        }
    }
}
