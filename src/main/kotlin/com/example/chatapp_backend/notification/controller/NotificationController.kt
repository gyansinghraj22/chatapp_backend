package com.example.chatapp_backend.notification.controller

import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.notification.dto.*
import com.example.chatapp_backend.notification.service.DeviceTokenService
import com.example.chatapp_backend.notification.service.NotificationPreferenceService
import com.example.chatapp_backend.persistence.entity.NotificationEntity
import com.example.chatapp_backend.persistence.repository.NotificationRepository
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val devices: DeviceTokenService,
    private val preferences: NotificationPreferenceService,
    private val notifications: NotificationRepository
) {
    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun registerDevice(@AuthenticationPrincipal user: AuthenticatedUser, @Valid @RequestBody request: RegisterDeviceRequest) =
        devices.register(user.id, request)

    @DeleteMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unregisterDevice(@AuthenticationPrincipal user: AuthenticatedUser, @RequestParam token: String) =
        devices.unregister(user.id, token)

    @GetMapping("/preferences")
    fun getPreferences(@AuthenticationPrincipal user: AuthenticatedUser) = preferences.get(user.id)

    @PutMapping("/preferences")
    fun updatePreferences(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @Valid @RequestBody request: UpdateNotificationPreferencesRequest
    ) = preferences.update(user.id, request)

    @GetMapping
    fun list(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<InAppNotificationResponse> {
        if (page < 0 || size !in 1..100) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size between 1 and 100")
        return notifications.findAllByUser_IdOrderByCreatedAtDesc(user.id, PageRequest.of(page, size)).map(::toResponse)
    }

    @PatchMapping("/{notificationId}/read")
    fun markRead(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable notificationId: UUID): InAppNotificationResponse {
        val notification = notifications.findById(notificationId)
            .filter { it.user?.id == user.id }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found") }
        notification.isRead = true
        return toResponse(notifications.save(notification))
    }

    private fun toResponse(value: NotificationEntity) = InAppNotificationResponse(
        id = value.id.toString(), title = value.title, body = value.body,
        type = value.notificationType, roomId = value.room?.id?.toString(),
        messageId = value.message?.id?.toString(), read = value.isRead, createdAt = value.createdAt
    )
}
