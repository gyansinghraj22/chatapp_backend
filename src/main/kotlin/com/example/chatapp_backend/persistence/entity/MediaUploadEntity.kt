package com.example.chatapp_backend.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "media_uploads",
    indexes = [
        Index(name = "ix_media_uploads_owner_created", columnList = "owner_id, created_at DESC"),
        Index(name = "ix_media_uploads_expiration", columnList = "expires_at")
    ]
)
class MediaUploadEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = ForeignKey(name = "fk_media_uploads_owner"))
    var owner: UserEntity? = null,

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    var storageKey: String = "",

    @Column(name = "original_file_name", nullable = false, length = 255)
    var originalFileName: String = "",

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String = "",

    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0,

    @Column(name = "sha256", nullable = false, length = 64)
    var sha256: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: MediaUploadStatus = MediaUploadStatus.UPLOADED,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now().plusSeconds(86_400)
) : AuditedEntity()
