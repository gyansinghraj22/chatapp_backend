package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "message_reactions",
    indexes = [Index(name = "idx_message_reactions_message_id", columnList = "message_id")]
)
class MessageReaction(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, foreignKey = ForeignKey(name = "fk_message_reactions_message"))
    var message: Message,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_message_reactions_user"))
    var user: User,

    @Column(name = "reaction", nullable = false, length = 20)
    var reaction: String,
) : AuditableEntity()
