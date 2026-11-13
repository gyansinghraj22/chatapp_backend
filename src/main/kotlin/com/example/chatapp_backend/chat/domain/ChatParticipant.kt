package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.chat.enums.ChatParticipantRole
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "chat_participants",
    indexes = [
        Index(name = "idx_chat_participants_user_room", columnList = "user_id, room_id"),
        Index(name = "idx_chat_participants_room_user", columnList = "room_id, user_id")
    ]
)
class ChatParticipant(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = ForeignKey(name = "fk_chat_participants_room"))
    var room: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_chat_participants_user"))
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: ChatParticipantRole = ChatParticipantRole.MEMBER,

    @Column(name = "joined_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    var joinedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "left_at", columnDefinition = "TIMESTAMPTZ")
    var leftAt: OffsetDateTime? = null,

    @Column(name = "last_read_message_id")
    var lastReadMessageId: UUID? = null,

    @Column(name = "is_muted", nullable = false)
    var isMuted: Boolean = false,

    @Column(name = "is_archived", nullable = false)
    var isArchived: Boolean = false,
) : AuditableEntity()
