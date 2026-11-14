package com.example.chatapp_backend.chat.dto

import com.example.chatapp_backend.persistence.entity.ParticipantRole
import com.example.chatapp_backend.persistence.entity.RoomType
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateGroupRoomRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 500) val description: String? = null,
    @field:Size(max = 99) val participantIds: Set<UUID> = emptySet()
)

data class CreatePrivateRoomRequest(@field:NotEmpty @field:Size(max = 1) val participantIds: Set<UUID>)
data class AddParticipantsRequest(@field:NotEmpty @field:Size(max = 99) val userIds: Set<UUID>)
data class UpdateParticipantRoleRequest(val role: ParticipantRole)

data class UpdateGroupRoomRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 500) val description: String? = null,
    @field:Size(max = 2048) val groupPictureUrl: String? = null
)

data class ParticipantSettingsRequest(val isMuted: Boolean, val isArchived: Boolean)

data class ParticipantResponse(
    val userId: UUID,
    val username: String,
    val fullName: String?,
    val role: ParticipantRole,
    val joinedAt: Instant,
    val isMuted: Boolean,
    val isArchived: Boolean
)

data class RoomResponse(
    val id: UUID,
    val type: RoomType,
    val name: String?,
    val description: String?,
    val groupPictureUrl: String?,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val participants: List<ParticipantResponse>
)

data class RoomPageResponse(
    val rooms: List<RoomResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
