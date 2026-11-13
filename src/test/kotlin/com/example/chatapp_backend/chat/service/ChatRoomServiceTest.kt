package com.example.chatapp_backend.chat.service

import com.example.chatapp_backend.chat.dto.CreateGroupRoomRequest
import com.example.chatapp_backend.persistence.entity.*
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

class ChatRoomServiceTest {
    private val rooms = Mockito.mock(ChatRoomRepository::class.java)
    private val participants = Mockito.mock(ChatParticipantRepository::class.java)
    private val users = Mockito.mock(UserRepository::class.java)
    private val service = ChatRoomService(rooms, participants, users)

    @Test
    fun `group creation assigns owner and includes creator`() {
        val creatorId = UUID.randomUUID()
        val invitedId = UUID.randomUUID()
        val creator = UserEntity(username = "creator", email = "creator@example.test").also { it.id = creatorId }
        val invited = UserEntity(username = "invited", email = "invited@example.test").also { it.id = invitedId }
        val room = ChatRoomEntity(name = "team", roomType = RoomType.GROUP, createdBy = creator)
        Mockito.`when`(users.findById(creatorId)).thenReturn(Optional.of(creator))
        Mockito.`when`(users.findAllById(Mockito.anyCollection())).thenReturn(listOf(creator, invited))
        Mockito.`when`(rooms.saveAndFlush(Mockito.any(ChatRoomEntity::class.java))).thenReturn(room)
        Mockito.`when`(participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id)).thenReturn(
            listOf(ChatParticipantEntity(room = room, user = creator, role = ParticipantRole.OWNER), ChatParticipantEntity(room = room, user = invited))
        )

        val result = service.createGroup(creatorId, CreateGroupRoomRequest(" team ", participantIds = setOf(invitedId)))

        assertEquals(RoomType.GROUP, result.type)
        assertEquals(2, result.participants.size)
        assertEquals(ParticipantRole.OWNER, result.participants.first { it.userId == creatorId }.role)
    }

    @Test
    fun `group membership requires real users`() {
        val creatorId = UUID.randomUUID()
        val creator = UserEntity(username = "creator", email = "creator@example.test").also { it.id = creatorId }
        Mockito.`when`(users.findById(creatorId)).thenReturn(Optional.of(creator))
        Mockito.`when`(users.findAllById(Mockito.anyCollection())).thenReturn(listOf(creator))

        assertThrows(ResponseStatusException::class.java) {
            service.createGroup(creatorId, CreateGroupRoomRequest("team", participantIds = setOf(UUID.randomUUID())))
        }
    }
}
