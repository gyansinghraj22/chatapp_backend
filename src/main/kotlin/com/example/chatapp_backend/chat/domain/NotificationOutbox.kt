package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "notification_outbox",
    indexes = [Index(name = "idx_notification_outbox_status", columnList = "status, created_at")]
)
class NotificationOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false, foreignKey = ForeignKey(name = "fk_notification_outbox_notification"))
    var notification: Notification,

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "PENDING",

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "dispatched_at", columnDefinition = "TIMESTAMPTZ")
    var dispatchedAt: OffsetDateTime? = null,
) : AuditableEntity()
