package com.example.chatapp_backend.auth.domain

import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")]
)
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_refresh_tokens_user"))
    var user: User,

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    var expiresAt: OffsetDateTime,

    @Column(name = "revoked_at", columnDefinition = "TIMESTAMPTZ")
    var revokedAt: OffsetDateTime? = null
) : AuditableEntity()
