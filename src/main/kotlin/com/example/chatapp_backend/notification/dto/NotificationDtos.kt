package com.example.chatapp_backend.notification.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterDeviceRequest(
    @field:NotBlank @field:Size(max = 4096) val token: String,
    @field:NotBlank val platform: String
)

data class NotificationPreferencesResponse(
    val pushEnabled: Boolean,
    val messageNotificationsEnabled: Boolean,
    val groupNotificationsEnabled: Boolean
)

data class UpdateNotificationPreferencesRequest(
    val pushEnabled: Boolean = true,
    val messageNotificationsEnabled: Boolean = true,
    val groupNotificationsEnabled: Boolean = true
)

data class InAppNotificationResponse(
    val id: String,
    val title: String,
    val body: String?,
    val type: String,
    val roomId: String?,
    val messageId: String?,
    val read: Boolean,
    val createdAt: Instant
)
