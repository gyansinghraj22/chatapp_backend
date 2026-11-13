package com.example.chatapp_backend.chat.service

import com.example.chatapp_backend.chat.dto.*
import com.example.chatapp_backend.persistence.entity.*
import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
import com.example.chatapp_backend.persistence.repository.ChatRoomRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class ChatRoomService(
    private val rooms: ChatRoomRepository,
    private val participants: ChatParticipantRepository,
    private val users: UserRepository
) {
    companion object {
        private const val MAX_GROUP_PARTICIPANTS = 100
        private const val MAX_PAGE_SIZE = 100
    }

    @Transactional
    fun createGroup(creatorId: UUID, request: CreateGroupRoomRequest): RoomResponse {
        val creator = activeUser(creatorId)
        val name = request.name.trim()
        if (name.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name is required")
        val memberIds = (request.participantIds + creatorId).toSet()
        if (memberIds.size > MAX_GROUP_PARTICIPANTS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A group can contain at most $MAX_GROUP_PARTICIPANTS active participants")
        }
        val memberUsers = loadActiveUsers(memberIds)
        val room = rooms.saveAndFlush(
            ChatRoomEntity(
                name = name,
                roomType = RoomType.GROUP,
                description = request.description?.trim()?.takeIf(String::isNotEmpty),
                createdBy = creator
            )
        )
        memberUsers.forEach { user ->
            participants.save(
                ChatParticipantEntity(
                    room = room,
                    user = user,
                    role = if (user.id == creatorId) ParticipantRole.OWNER else ParticipantRole.MEMBER
                )
            )
        }
        participants.flush()
        return roomResponse(room, participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id))
    }

    @Transactional
    fun createPrivateRoom(creatorId: UUID, request: CreatePrivateRoomRequest): RoomResponse {
        activeUser(creatorId)
        val memberIds = request.participantIds + creatorId
        if (memberIds.size != 2) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A private conversation must have exactly two distinct users")
        val memberUsers = loadActiveUsers(memberIds)
        val key = memberIds.map(UUID::toString).sorted().joinToString(":")
        rooms.findByDirectChatKey(key)?.let { room ->
            if (room.id !in listOf(null)) {
                val existingMembers = participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id)
                if (existingMembers.mapNotNull { it.user?.id }.toSet() == memberIds) return roomResponse(room, existingMembers)
            }
            throw ResponseStatusException(HttpStatus.CONFLICT, "Private room exists with inconsistent active membership")
        }
        val room = try {
            rooms.saveAndFlush(ChatRoomEntity(roomType = RoomType.PRIVATE, createdBy = users.findById(creatorId).orElseThrow(), directChatKey = key))
        } catch (_: DataIntegrityViolationException) {
            // Concurrent creation of the same pair is resolved by the unique direct-chat key.
            rooms.findByDirectChatKey(key)?.let { existing ->
                val current = participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(existing.id)
                if (current.mapNotNull { it.user?.id }.toSet() == memberIds) return roomResponse(existing, current)
            }
            throw ResponseStatusException(HttpStatus.CONFLICT, "Unable to create private room")
        }
        memberUsers.forEach { user -> participants.save(ChatParticipantEntity(room = room, user = user, role = ParticipantRole.MEMBER)) }
        participants.flush()
        return roomResponse(room, participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id))
    }

    @Transactional(readOnly = true)
    fun getRoom(userId: UUID, roomId: UUID): RoomResponse {
        val room = room(roomId)
        requireMembership(roomId, userId)
        return roomResponse(room, participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId))
    }

    @Transactional(readOnly = true)
    fun listRooms(userId: UUID, page: Int, size: Int): RoomPageResponse {
        if (page < 0 || size !in 1..MAX_PAGE_SIZE) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and $MAX_PAGE_SIZE")
        val memberships = participants.findAllByUser_IdAndLeftAtIsNullOrderByJoinedAtDesc(userId, PageRequest.of(page, size))
        val roomResponses = memberships.content.mapNotNull { membership ->
            membership.room?.let { room ->
                roomResponse(room, participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id))
            }
        }
        return RoomPageResponse(roomResponses, memberships.number, memberships.size, memberships.totalElements, memberships.totalPages)
    }

    @Transactional(readOnly = true)
    fun listParticipants(userId: UUID, roomId: UUID): List<ParticipantResponse> {
        requireMembership(roomId, userId)
        return participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId).map(::participantResponse)
    }

    @Transactional
    fun addParticipants(actorId: UUID, roomId: UUID, userIds: Set<UUID>): List<ParticipantResponse> {
        val room = room(roomId)
        requireGroup(room)
        val actor = requireMembership(roomId, actorId)
        requireAdmin(actor)
        if (userIds.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one participant is required")
        val current = participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId)
        val newIds = userIds - current.mapNotNull { it.user?.id }.toSet()
        if (current.size + newIds.size > MAX_GROUP_PARTICIPANTS) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A group can contain at most $MAX_GROUP_PARTICIPANTS active participants")
        val newUsers = loadActiveUsers(newIds)
        newUsers.forEach { user -> participants.save(ChatParticipantEntity(room = room, user = user, role = ParticipantRole.MEMBER)) }
        participants.flush()
        return participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId).map(::participantResponse)
    }

    @Transactional
    fun removeParticipant(actorId: UUID, roomId: UUID, targetId: UUID) {
        val room = room(roomId)
        requireGroup(room)
        val actor = requireMembership(roomId, actorId)
        val target = requireMembership(roomId, targetId)
        authorizeRemoval(actor, target, actorId, targetId)
        if (target.role == ParticipantRole.OWNER) throw ResponseStatusException(HttpStatus.CONFLICT, "Transfer ownership before removing the group owner")
        target.leftAt = Instant.now()
        participants.save(target)
    }

    @Transactional
    fun leaveRoom(userId: UUID, roomId: UUID) {
        val room = room(roomId)
        if (room.roomType == RoomType.PRIVATE) throw ResponseStatusException(HttpStatus.CONFLICT, "Leaving a private conversation is not supported; archive it instead")
        val membership = requireMembership(roomId, userId)
        if (membership.role == ParticipantRole.OWNER) {
            val anotherOwner = participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId)
                .firstOrNull { it.user?.id != userId && it.role == ParticipantRole.OWNER }
            if (anotherOwner == null) throw ResponseStatusException(HttpStatus.CONFLICT, "Transfer ownership before leaving the group")
        }
        membership.leftAt = Instant.now()
        participants.save(membership)
    }

    @Transactional
    fun updateRole(actorId: UUID, roomId: UUID, targetId: UUID, role: ParticipantRole) {
        val room = room(roomId)
        requireGroup(room)
        val actor = requireMembership(roomId, actorId)
        if (actor.role != ParticipantRole.OWNER) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can change participant roles")
        if (targetId == actorId && role != ParticipantRole.OWNER) throw ResponseStatusException(HttpStatus.CONFLICT, "Transfer ownership to another member before changing your own role")
        val target = requireMembership(roomId, targetId)
        if (role == ParticipantRole.OWNER) {
            actor.role = ParticipantRole.ADMIN
            participants.save(actor)
        }
        target.role = role
        participants.save(target)
    }

    private fun loadActiveUsers(ids: Set<UUID>): List<UserEntity> {
        val found = users.findAllById(ids).filter { it.isActive }
        if (found.map { it.id }.toSet() != ids) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more users do not exist or are inactive")
        return found
    }

    private fun activeUser(id: UUID): UserEntity = users.findById(id).filter { it.isActive }
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or inactive") }

    private fun room(id: UUID): ChatRoomEntity = rooms.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found") }

    private fun requireMembership(roomId: UUID, userId: UUID): ChatParticipantEntity =
        participants.findByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Active room membership is required")

    private fun requireGroup(room: ChatRoomEntity) {
        if (room.roomType != RoomType.GROUP) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Participant management is only available for group rooms")
    }

    private fun requireAdmin(participant: ChatParticipantEntity) {
        if (participant.role != ParticipantRole.OWNER && participant.role != ParticipantRole.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Group administrator permissions are required")
        }
    }

    private fun authorizeRemoval(actor: ChatParticipantEntity, target: ChatParticipantEntity, actorId: UUID, targetId: UUID) {
        if (actorId == targetId) return
        if (actor.role == ParticipantRole.OWNER) return
        if (actor.role == ParticipantRole.ADMIN && target.role == ParticipantRole.MEMBER) return
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient group permissions to remove this participant")
    }

    private fun roomResponse(room: ChatRoomEntity, members: List<ChatParticipantEntity>) = RoomResponse(
        id = room.id,
        type = room.roomType,
        name = room.name,
        description = room.description,
        groupPictureUrl = room.groupPictureUrl,
        createdBy = room.createdBy?.id ?: throw IllegalStateException("Room creator is missing"),
        createdAt = room.createdAt,
        updatedAt = room.updatedAt,
        participants = members.map(::participantResponse)
    )

    private fun participantResponse(participant: ChatParticipantEntity): ParticipantResponse {
        val user = participant.user ?: throw IllegalStateException("Participant user is missing")
        return ParticipantResponse(user.id, user.username, user.fullName, participant.role, participant.joinedAt, participant.isMuted, participant.isArchived)
    }
}
