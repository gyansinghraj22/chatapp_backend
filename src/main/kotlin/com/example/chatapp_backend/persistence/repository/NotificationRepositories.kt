package com.example.chatapp_backend.persistence.repository

import com.example.chatapp_backend.persistence.entity.NotificationPreferenceEntity
import com.example.chatapp_backend.persistence.entity.NotificationOutboxEntity
import com.example.chatapp_backend.persistence.entity.NotificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationPreferenceRepository : JpaRepository<NotificationPreferenceEntity, UUID> {
    fun findByUser_Id(userId: UUID): NotificationPreferenceEntity?
}

interface NotificationEntityRepository : JpaRepository<NotificationEntity, UUID>
interface NotificationOutboxEntityRepository : JpaRepository<NotificationOutboxEntity, UUID>
