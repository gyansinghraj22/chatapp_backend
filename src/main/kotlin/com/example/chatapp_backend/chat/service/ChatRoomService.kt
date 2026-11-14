package com.example.chatapp_backend/chat/service

import com.example.chatapp_backend.chat.dto.*
import com.example.chatapp_backend.persistence.entity.*
import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
import com.example.chatapp_backend.persistence.repository.ChatRoomRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
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
    companion object { private const val MAX_GROUP_PARTICIPANTS = 100; private const val MAX_PAGE_SIZE = 100 }

    @Transactional
    fun createGroup(creatorId: UUID, request: CreateGroupRoomRequest): RoomResponse {
        val creator = activeUser(creatorId)
        val name = request.name.trim()
        if (name.isBlank()) badRequest("Group name is required")
        val memberIds = request.participantIds + creatorId
        if (memberIds.size > MAX_GROUP_PARTICIPANTS) badRequest("A group can contain at most $MAX_GROUP_PARTICIPANTS active participants")
        val memberUsers = loadActiveUsers(memberIds)
        val room = rooms.saveAndFlush(ChatRoomEntity(name = name, roomType = RoomType.GROUP,
            description = request.description?.trim()?.takeIf(String::isNotEmpty), createdBy = creator))
        memberUsers.forEach { user -> participants.save(ChatParticipantEntity(
            room = room, user = user, role = if (user.id == creatorId) ParticipantRole.OWNER else ParticipantRole.MEMBER
        )) }
        participants.flush()
        return roomResponse(room, participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id))
    }

    @Transactional
    fun createPrivateRoom(creatorId: UUID, request: CreatePrivateRoomRequest): RoomResponse {
        activeUser(creatorId)
        if (request.participantIds.size != 1 || creatorId in request.participantIds) badRequest("A private conversation requires exactly one different user")
        val memberIds = request.participantIds + creatorId
        loadActiveUsers(memberIds)
        val key = memberIds.map(UUID::toString).sorted().joinToString(":")
        rooms.insertPrivateRoomIfAbsent(UUID.randomUUID(), creatorId, key)
        val room = rooms.findByDirectChatKey(key) ?: throw IllegalStateException("Private room creation failed")
        memberIds.forEach { participants.insertActiveMemberIfAbsent(UUID.randomUUID(), room.id, it) }
        val active = participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(room.id)
        if (active.mapNotNull { it.user?.id }.toSet() != memberIds) conflict("Private room membership is inconsistent")
        return roomResponse(room, active)
    }

    @Transactional(readOnly = true)
    fun getRoom(userId: UUID, roomId: UUID): RoomResponse {
        val room = room(roomId); requireMembership(roomId, userId)
        return roomResponse(room, participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId))
    }

    @Transactional(readOnly = true)
    fun listRooms(userId: UUID, page: Int, size: Int): RoomPageResponse {
        validatePage(page, size)
        val result = participants.findAllByUser_IdAndLeftAtIsNullOrderByJoinedAtDesc(userId, PageRequest.of(page, size))
        val roomIds = result.content.mapNotNull { it.room?.id }.distinct()
        val members = if (roomIds.isEmpty()) emptyMap() else participants.findAllByRoom_IdInAndLeftAtIsNullOrderByJoinedAtAsc(roomIds).groupBy { it.room?.id }
        val responses = result.content.mapNotNull { membership -> membership.room?.let { roomResponse(it, members[it.id].orEmpty()) } }
        return RoomPageResponse(responses, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional(readOnly = true)
    fun listParticipants(userId: UUID, roomId: UUID): List<ParticipantResponse> {
        requireMembership(roomId, userId)
        return participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId).map(::participantResponse)
    }

    @Transactional
    fun updateGroup(actorId: UUID, roomId: UUID, request: UpdateGroupRoomRequest): RoomResponse {
        val room = lockedRoom(roomId); requireGroup(room); requireAdmin(requireMembership(roomId, actorId))
        val name = request.name.trim()
        if (name.isBlank()) badRequest("Group name is required")
        room.name = name
        room.description = request.description?.trim()?.takeIf(String::isNotEmpty)
        room.groupPictureUrl = request.groupPictureUrl?.trim()?.takeIf(String::isNotEmpty)
        rooms.save(room)
        return roomResponse(room, participants.findActiveForRoomForUpdate(roomId))
    }

    @Transactional
    fun addParticipants(actorId: UUID, roomId: UUID, userIds: Set<UUID>): List<ParticipantResponse> {
        val room = lockedRoom(roomId); requireGroup(room); requireAdmin(requireMembership(roomId, actorId))
        if (userIds.isEmpty()) badRequest("At least one participant is required")
        val candidateUsers = loadActiveUsers(userIds)
        val current = participants.findActiveForRoomForUpdate(roomId)
        val activeIds = current.mapNotNull { it.user?.id }.toSet()
        val additions = candidateUsers.filter { it.id !in activeIds }
        if (current.size + additions.size > MAX_GROUP_PARTICIPANTS) badRequest("A group can contain at most $MAX_GROUP_PARTICIPANTS active participants")
        additions.forEach { participants.save(ChatParticipantEntity(room = room, user = it, role = ParticipantRole.MEMBER)) }
        participants.flush()
        return participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId).map(::participantResponse)
    }

    @Transactional
    fun removeParticipant(actorId: UUID, roomId: UUID, targetId: UUID) {
        val room = lockedRoom(roomId); requireGroup(room)
        val active = participants.findActiveForRoomForUpdate(roomId)
        val actor = active.firstOrNull { it.user?.id == actorId } ?: forbidden("Active room membership is required")
        val target = active.firstOrNull { it.user?.id == targetId } ?: forbidden("Target is not an active participant")
        if (actorId != targetId) {
            when {
                actor.role == ParticipantRole.OWNER && target.role != ParticipantRole.OWNER -> Unit
                actor.role == ParticipantRole.ADMIN && target.role == ParticipantRole.MEMBER -> Unit
                else -> forbidden("Insufficient group permissions to remove this participant")
            }
        } else if (actor.role == ParticipantRole.OWNER && active.none { it.user?.id != actorId && it.role == ParticipantRole.OWNER }) {
            conflict("Transfer ownership before leaving the group")
        }
        target.leftAt = Instant.now(); participants.save(target)
    }

    @Transactional
    fun leaveRoom(userId: UUID, roomId: UUID) {
        val room = lockedRoom(roomId)
        if (room.roomType == RoomType.PRIVATE) conflict("Private conversations cannot be left; archive it instead")
        val active = participants.findActiveForRoomForUpdate(roomId)
        val target = active.firstOrNull { it.user?.id == userId } ?: forbidden("Active room membership is required")
        if (target.role == ParticipantRole.OWNER && active.none { it.user?.id != userId && it.role == ParticipantRole.OWNER }) conflict("Transfer ownership before leaving the group")
        target.leftAt = Instant.now(); participants.save(target)
    }

    @Transactional
    fun updateRole(actorId: UUID, roomId: UUID, targetId: UUID, role: ParticipantRole) {
        val room = lockedRoom(roomId); requireGroup(room)
        val active = participants.findActiveForRoomForUpdate(roomId)
        val actor = active.firstOrNull { it.user?.id == actorId } ?: forbidden("Active room membership is required")
        if (actor.role != ParticipantRole.OWNER) forbidden("Only the owner can change participant roles")
        if (role == ParticipantRole.OWNER) conflict("Use the ownership transfer endpoint")
        if (actorId == targetId) conflict("Owner cannot demote themselves")
        val target = active.firstOrNull { it.user?.id == targetId } ?: forbidden("Target is not an active participant")
        if (target.role == ParticipantRole.OWNER) conflict("Transfer ownership before changing the current owner role")
        target.role = role; participants.save(target)
    }

    @Transactional
    fun transferOwnership(actorId: UUID, roomId: UUID, newOwnerId: UUID) {
        val room = lockedRoom(roomId); requireGroup(room)
        val active = participants.findActiveForRoomForUpdate(roomId)
        val owner = active.firstOrNull { it.user?.id == actorId } ?: forbidden("Active room membership is required")
        if (owner.role != ParticipantRole.OWNER) forbidden("Only the current owner can transfer ownership")
        val successor = active.firstOrNull { it.user?.id == newOwnerId } ?: badRequest("New owner must be an active participant")
        if (newOwnerId == actorId) badRequest("New owner must be a different participant")
        owner.role = ParticipantRole.ADMIN; participants.saveAndFlush(owner)
        successor.role = ParticipantRole.OWNER; participants.saveAndFlush(successor)
    }

    @Transactional
    fun updateParticipantSettings(userId: UUID, roomId: UUID, request: ParticipantSettingsRequest): ParticipantResponse {
        room(roomId)
        val participant = requireMembership(roomId, userId)
        participant.isMuted = request.isMuted; participant.isArchived = request.isArchived
        return participantResponse(participants.save(participant))
    }

    private fun loadActiveUsers(ids: Set<UUID>): List<UserEntity> {
        val found = users.findAllById(ids).filter { it.isActive }
        if (found.map { it.id }.toSet() != ids) badRequest("One or more users do not exist or are inactive")
        return found
    }
    private fun activeUser(id: UUID): UserEntity = users.findById(id).filter { it.isActive }
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or inactive") }
    private fun room(id: UUID): ChatRoomEntity = rooms.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found") }
    private fun lockedRoom(id: UUID): ChatRoomEntity = rooms.findByIdForUpdate(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")
    private fun requireMembership(roomId: UUID, userId: UUID): ChatParticipantEntity = participants.findByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, userId) ?: forbidden("Active room membership is required")
    private fun requireGroup(room: ChatRoomEntity) { if (room.roomType != RoomType.GROUP) badRequest("Operation is only available for group rooms") }
    private fun requireAdmin(p: ChatParticipantEntity) { if (p.role != ParticipantRole.OWNER && p.role != ParticipantRole.ADMIN) forbidden("Group administrator permissions are required") }
    private fun validatePage(page: Int, size: Int) { if (page < 0 || size !in 1..MAX_PAGE_SIZE) badRequest("Page must be non-negative and size must be between 1 and $MAX_PAGE_SIZE") }
    private fun roomResponse(room: ChatRoomEntity, members: List<ChatParticipantEntity>) = RoomResponse(
        room.id, room.roomType, room.name, room.description, room.groupPictureUrl,
        room.createdBy?.id ?: throw IllegalStateException("Room creator is missing"), room.createdAt, room.updatedAt,
        members.map(::participantResponse)
    )
    private fun participantResponse(p: ChatParticipantEntity): ParticipantResponse {
        val user = p.user ?: throw IllegalStateException("Participant user is missing")
        return ParticipantResponse(user.id, user.username, user.fullName, p.role, p.joinedAt, p.isMuted, p.isArchived)
    }
    private fun badRequest(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
    private fun forbidden(message: String): Nothing = throw ResponseStatusException(HttpStatus.FORBIDDEN, message)
    private fun conflict(message: String): Nothing = throw ResponseStatusException(HttpStatus.CONFLICT, message)
}
