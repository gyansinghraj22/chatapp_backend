package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.chat.enums.ChatRoomType
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "chat_rooms",
    indexes = [
        Index(name = "idx_chat_rooms_created_by", columnList = "created_by"),
        Index(name = "idx_chat_rooms_room_type", columnList = "room_type")
    ]
)
class ChatRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "name", length = 100)
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    var roomType: ChatRoomType = ChatRoomType.PRIVATE,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "group_picture_url", columnDefinition = "TEXT")
    var groupPictureUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = ForeignKey(name = "fk_chat_rooms_created_by"))
    var createdBy: User,

    @Column(name = "direct_chat_key", unique = true, length = 100)
    var directChatKey: String? = null,

    @Column(name = "last_message_id")
    var lastMessageId: UUID? = null,
) : AuditableEntity()
