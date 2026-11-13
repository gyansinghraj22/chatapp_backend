package com.example.chatapp_backend.notification.service

import com.example.chatapp_backend.notification.dto.NotificationPreferencesResponse
import com.example.chatapp_backend.notification.dto.UpdateNotificationPreferencesRequest
import com.example.chatapp_backend.persistence.entity.NotificationPreferenceEntity
import com.example.chatapp_backend.persistence.repository.NotificationPreferenceRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class NotificationPreferenceService(
    private val preferences: NotificationPreferenceRepository,
    private val users: UserRepository
) {
    @Transactional(readOnly = true)
    fun get(userId: UUID): NotificationPreferencesResponse {
        val user = users.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val pref = preferences.findByUser_Id(userId)
        return NotificationPreferencesResponse(
            pushEnabled = pref?.pushEnabled ?: true,
            messageNotificationsEnabled = pref?.messageNotificationsEnabled ?: true,
            groupNotificationsEnabled = pref?.groupNotificationsEnabled ?: true
        )
    }

    @Transactional
    fun update(userId: UUID, request: UpdateNotificationPreferencesRequest): NotificationPreferencesResponse {
        val user = users.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val pref = preferences.findByUser_Id(userId) ?: NotificationPreferenceEntity(user = user)
        pref.pushEnabled = request.pushEnabled
        pref.messageNotificationsEnabled = request.messageNotificationsEnabled
        pref.groupNotificationsEnabled = request.groupNotificationsEnabled
        preferences.save(pref)
        return NotificationPreferencesResponse(pref.pushEnabled, pref.messageNotificationsEnabled, pref.groupNotificationsEnabled)
    }
}
