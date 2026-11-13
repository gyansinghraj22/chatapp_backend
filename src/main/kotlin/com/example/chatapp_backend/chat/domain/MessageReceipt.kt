package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "message_receipts",
    indexes = [Index(name = "idx_message_receipts_message_recipient", columnList = "message_id, recipient_id", unique = true)]
)
class MessageReceipt(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, foreignKey = ForeignKey(name = "fk_message_receipts_message"))
    var message: Message,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, foreignKey = ForeignKey(name = "fk_message_receipts_recipient"))
    var recipient: User,

    @Column(name = "delivered_at", columnDefinition = "TIMESTAMPTZ")
    var deliveredAt: OffsetDateTime? = null,

    @Column(name = "read_at", columnDefinition = "TIMESTAMPTZ")
    var readAt: OffsetDateTime? = null,
) : AuditableEntity()
