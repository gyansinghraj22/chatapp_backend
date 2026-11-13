package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.chat.enums.ReportStatus
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "message_reports",
    indexes = [Index(name = "idx_message_reports_status", columnList = "status, created_at DESC")]
)
class MessageReport(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, foreignKey = ForeignKey(name = "fk_message_reports_message"))
    var message: Message,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, foreignKey = ForeignKey(name = "fk_message_reports_reporter"))
    var reporter: User,

    @Column(name = "reason", nullable = false, length = 100)
    var reason: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReportStatus = ReportStatus.PENDING,

    @Column(name = "reviewed_at", columnDefinition = "TIMESTAMPTZ")
    var reviewedAt: OffsetDateTime? = null,
) : AuditableEntity()
