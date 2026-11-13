package com.example.chatapp_backend.chat.controller

import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.chat.dto.*
import com.example.chatapp_backend.chat.service.ChatRoomService
import com.example.chatapp_backend.persistence.entity.ParticipantRole
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/rooms")
class ChatRoomController(private val chatRoomService: ChatRoomService) {
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroup(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: CreateGroupRoomRequest
    ): RoomResponse = chatRoomService.createGroup(principal.id, request)

    @PostMapping("/private")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPrivate(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: CreatePrivateRoomRequest
    ): RoomResponse = chatRoomService.createPrivateRoom(principal.id, request)

    @GetMapping
    fun listRooms(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): RoomPageResponse = chatRoomService.listRooms(principal.id, page, size)

    @GetMapping("/{roomId}")
    fun getRoom(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID): RoomResponse =
        chatRoomService.getRoom(principal.id, roomId)

    @GetMapping("/{roomId}/participants")
    fun listParticipants(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID): List<ParticipantResponse> =
        chatRoomService.listParticipants(principal.id, roomId)

    @PostMapping("/{roomId}/participants")
    fun addParticipants(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable roomId: UUID,
        @Valid @RequestBody request: AddParticipantsRequest
    ): List<ParticipantResponse> = chatRoomService.addParticipants(principal.id, roomId, request.userIds)

    @DeleteMapping("/{roomId}/participants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeParticipant(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable roomId: UUID,
        @PathVariable userId: UUID
    ) = chatRoomService.removeParticipant(principal.id, roomId, userId)

    @PostMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leave(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID) =
        chatRoomService.leaveRoom(principal.id, roomId)

    @PatchMapping("/{roomId}/participants/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateRole(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable roomId: UUID,
        @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateParticipantRoleRequest
    ) = chatRoomService.updateRole(principal.id, roomId, userId, request.role)
}
