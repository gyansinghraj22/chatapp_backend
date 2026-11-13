package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.chat.enums.MessageType
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_messages_room_created", columnList = "room_id, created_at DESC, id DESC"),
        Index(name = "idx_messages_sender_client_id", columnList = "sender_id, client_message_id", unique = true),
        Index(name = "idx_messages_sender_created", columnList = "sender_id, created_at DESC"),
        Index(name = "idx_messages_reply_to", columnList = "reply_to_message_id")
    ]
)
class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "client_message_id", nullable = false)
    var clientMessageId: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = ForeignKey(name = "fk_messages_room"))
    var room: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = ForeignKey(name = "fk_messages_sender"))
    var sender: User,

    @Column(name = "content", columnDefinition = "TEXT")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    var messageType: MessageType = MessageType.TEXT,

    @Column(name = "reply_to_message_id")
    var replyToMessageId: UUID? = null,

    @Column(name = "is_edited", nullable = false)
    var isEdited: Boolean = false,

    @Column(name = "edited_at", columnDefinition = "TIMESTAMPTZ")
    var editedAt: OffsetDateTime? = null,

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    var deletedAt: OffsetDateTime? = null,
) : AuditableEntity()
