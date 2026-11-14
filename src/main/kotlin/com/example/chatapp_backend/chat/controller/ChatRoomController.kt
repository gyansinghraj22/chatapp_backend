package com.example.chatapp_backend.chat.controller

import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.chat.dto.*
import com.example.chatapp_backend.chat.service.ChatRoomService
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
    fun createGroup(@AuthenticationPrincipal principal: AuthenticatedUser, @Valid @RequestBody request: CreateGroupRoomRequest) =
        chatRoomService.createGroup(principal.id, request)

    @PostMapping("/private")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPrivate(@AuthenticationPrincipal principal: AuthenticatedUser, @Valid @RequestBody request: CreatePrivateRoomRequest) =
        chatRoomService.createPrivateRoom(principal.id, request)

    @GetMapping
    fun listRooms(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = chatRoomService.listRooms(principal.id, page, size)

    @GetMapping("/{roomId}")
    fun getRoom(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID) =
        chatRoomService.getRoom(principal.id, roomId)

    @PatchMapping("/{roomId}")
    fun updateGroup(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID,
                    @Valid @RequestBody request: UpdateGroupRoomRequest) = chatRoomService.updateGroup(principal.id, roomId, request)

    @GetMapping("/{roomId}/participants")
    fun listParticipants(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID) =
        chatRoomService.listParticipants(principal.id, roomId)

    @PostMapping("/{roomId}/participants")
    fun addParticipants(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID,
                        @Valid @RequestBody request: AddParticipantsRequest) = chatRoomService.addParticipants(principal.id, roomId, request.userIds)

    @DeleteMapping("/{roomId}/participants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeParticipant(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID,
                          @PathVariable userId: UUID) = chatRoomService.removeParticipant(principal.id, roomId, userId)

    @PostMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leave(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID) =
        chatRoomService.leaveRoom(principal.id, roomId)

    @PatchMapping("/{roomId}/participants/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateRole(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID,
                   @PathVariable userId: UUID, @Valid @RequestBody request: UpdateParticipantRoleRequest) =
        chatRoomService.updateRole(principal.id, roomId, userId, request.role)

    @PostMapping("/{roomId}/owner/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun transferOwnership(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID,
                          @PathVariable userId: UUID) = chatRoomService.transferOwnership(principal.id, roomId, userId)

    @PatchMapping("/{roomId}/me/settings")
    fun updateMySettings(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable roomId: UUID,
                         @Valid @RequestBody request: ParticipantSettingsRequest) =
        chatRoomService.updateParticipantSettings(principal.id, roomId, request)
}
