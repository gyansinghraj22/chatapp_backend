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
    fun `group creation sets creator as owner and initial members as members`() {
        val creatorId = UUID.randomUUID()
        val inviteId = UUID.randomUUID()
        val creator = UserEntity(username = "creator", email = "creator@example.test").also { it.id = creatorId }
        val invite = UserEntity(username = "invite", email = "invite@example.test").also { it.id = inviteId }
        val room = ChatRoomEntity(name = "group", roomType = RoomType.GROUP, createdBy = creator)
        Mockito.`when`(users.findById(creatorId)).thenReturn(Optional.of(creator))
        Mockito.`when`(users.findAllById(Mockito.anyCollection())).thenReturn(listOf(creator, invite))
        Mockito.`when`(rooms.saveAndFlush(Mockito.any(ChatRoomEntity::class.java))).thenReturn(room)
        Mockito.`when`(participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id)).thenReturn(
            listOf(ChatParticipantEntity(room = room, user = creator, role = ParticipantRole.OWNER), ChatParticipantEntity(room = room, user = invite))
        )
        val response = service.createGroup(creatorId, CreateGroupRoomRequest(" group ", participantIds = setOf(inviteId)))
        assertEquals("group", response.name)
        assertEquals(ParticipantRole.OWNER, response.participants.first { it.userId == creatorId }.role)
        assertEquals(ParticipantRole.MEMBER, response.participants.first { it.userId == inviteId }.role)
    }

    @Test
    fun `group rejects unavailable invited users`() {
        val creatorId = UUID.randomUUID()
        val creator = UserEntity(username = "creator", email = "creator@example.test").also { it.id = creatorId }
        Mockito.`when`(users.findById(creatorId)).thenReturn(Optional.of(creator))
        Mockito.`when`(users.findAllById(Mockito.anyCollection())).thenReturn(listOf(creator))
        assertThrows(ResponseStatusException::class.java) {
            service.createGroup(creatorId, CreateGroupRoomRequest("team", participantIds = setOf(UUID.randomUUID())))
        }
    }
}
