package com.example.chatapp_backend.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "notification_preferences")
class NotificationPreferenceEntity(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: UserEntity? = null,

    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = true,

    @Column(name = "message_notifications_enabled", nullable = false)
    var messageNotificationsEnabled: Boolean = true,

    @Column(name = "group_notifications_enabled", nullable = false)
    var groupNotificationsEnabled: Boolean = true
) : AuditedEntity()
