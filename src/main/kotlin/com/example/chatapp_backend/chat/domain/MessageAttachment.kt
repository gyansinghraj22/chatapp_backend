package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "message_attachments",
    indexes = [Index(name = "idx_message_attachments_message_id", columnList = "message_id")]
)
class MessageAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, foreignKey = ForeignKey(name = "fk_message_attachments_message"))
    var message: Message,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
    var storageKey: String,

    @Column(name = "file_type", length = 100)
    var fileType: String? = null,

    @Column(name = "file_size")
    var fileSize: Long = 0L
) : AuditableEntity()
