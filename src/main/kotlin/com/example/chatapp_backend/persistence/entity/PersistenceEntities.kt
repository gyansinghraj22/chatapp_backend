package com.example.chatapp_backend.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID()
}

@MappedSuperclass
abstract class AuditedEntity : BaseEntity() {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PrePersist
    protected fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "users")
class UserEntity(
    @Column(name = "username", nullable = false, length = 50)
    var username: String = "",

    @Column(name = "email", nullable = false, length = 255)
    var email: String = "",

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Column(name = "full_name", length = 100)
    var fullName: String? = null,

    @Column(name = "profile_picture_url", columnDefinition = "text")
    var profilePictureUrl: String? = null,

    @Column(name = "bio", length = 500)
    var bio: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant? = null
) : AuditedEntity()

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null,

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    var tokenHash: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
) : BaseEntity() {
    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "chat_rooms")
class ChatRoomEntity(
    @Column(name = "name", length = 100)
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 10)
    var roomType: RoomType = RoomType.PRIVATE,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "group_picture_url", columnDefinition = "text")
    var groupPictureUrl: String? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    var createdBy: UserEntity? = null,

    @Column(name = "direct_chat_key", length = 100, unique = true)
    var directChatKey: String? = null
) : AuditedEntity()

@Entity
@Table(name = "chat_participants")
class ChatParticipantEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    var room: ChatRoomEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    var role: ParticipantRole = ParticipantRole.MEMBER,

    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now(),

    @Column(name = "left_at")
    var leftAt: Instant? = null,

    @Column(name = "last_read_at")
    var lastReadAt: Instant? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    var lastReadMessage: MessageEntity? = null,

    @Column(name = "is_muted", nullable = false)
    var isMuted: Boolean = false,

    @Column(name = "is_archived", nullable = false)
    var isArchived: Boolean = false
) : AuditedEntity()

@Entity
@Table(name = "messages")
class MessageEntity(
    @Column(name = "client_message_id", nullable = false, updatable = false)
    var clientMessageId: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    var room: ChatRoomEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    var sender: UserEntity? = null,

    @Column(name = "content", columnDefinition = "text")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 10)
    var messageType: MessageType = MessageType.TEXT,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    var replyToMessage: MessageEntity? = null,

    @Column(name = "is_edited", nullable = false)
    var isEdited: Boolean = false,

    @Column(name = "edited_at")
    var editedAt: Instant? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
) : AuditedEntity()

@Entity
@Table(name = "message_attachments")
class MessageAttachmentEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    var message: MessageEntity? = null,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String = "",

    @Column(name = "storage_key", nullable = false, columnDefinition = "text")
    var storageKey: String = "",

    @Column(name = "file_type", nullable = false, length = 100)
    var fileType: String = "",

    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0
) : BaseEntity() {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "message_reactions")
class MessageReactionEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    var message: MessageEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null,

    @Column(name = "reaction", nullable = false, length = 20)
    var reaction: String = ""
) : BaseEntity() {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "message_receipts")
class MessageReceiptEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    var message: MessageEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    var recipient: UserEntity? = null,

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,

    @Column(name = "read_at")
    var readAt: Instant? = null
) : AuditedEntity()

@Entity
@Table(name = "blocked_users")
class BlockedUserEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    var blocker: UserEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    var blockedUser: UserEntity? = null
) : BaseEntity() {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "message_reports")
class MessageReportEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    var message: MessageEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    var reporter: UserEntity? = null,

    @Column(name = "reason", nullable = false, length = 100)
    var reason: String = "",

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    var status: MessageReportStatus = MessageReportStatus.PENDING,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null
) : BaseEntity() {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    var sender: UserEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "room_id")
    var room: ChatRoomEntity? = null,

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "message_id")
    var message: MessageEntity? = null,

    @Column(name = "title", nullable = false, length = 255)
    var title: String = "",

    @Column(name = "body", columnDefinition = "text")
    var body: String? = null,

    @Column(name = "notification_type", nullable = false, length = 30)
    var notificationType: String = "",

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
) : BaseEntity() {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }
}

@Entity
@Table(name = "device_tokens")
class DeviceTokenEntity(
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String = "",

    @Column(name = "token_ciphertext", nullable = false, columnDefinition = "text")
    var tokenCiphertext: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    var platform: DevicePlatform = DevicePlatform.ANDROID,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : AuditedEntity()

@Entity
@Table(name = "notification_outbox")
class NotificationOutboxEntity(
    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false, unique = true)
    var notification: NotificationEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    var status: NotificationDispatchStatus = NotificationDispatchStatus.PENDING,

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: Instant = Instant.now(),

    @Column(name = "last_error", columnDefinition = "text")
    var lastError: String? = null
) : AuditedEntity()
