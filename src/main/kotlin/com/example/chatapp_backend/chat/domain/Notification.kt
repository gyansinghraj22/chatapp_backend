package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.chat.enums.NotificationType
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "notifications",
    indexes = [Index(name = "idx_notifications_user_unread", columnList = "user_id, is_read, created_at DESC")]
)
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_notifications_user"))
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", foreignKey = ForeignKey(name = "fk_notifications_sender"))
    var sender: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", foreignKey = ForeignKey(name = "fk_notifications_room"))
    var room: ChatRoom? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", foreignKey = ForeignKey(name = "fk_notifications_message"))
    var message: Message? = null,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "body", columnDefinition = "TEXT")
    var body: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    var notificationType: NotificationType,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
) : AuditableEntity()
