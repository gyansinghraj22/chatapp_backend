package com.example.chatapp_backend.auth.domain

import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_username", columnList = "username", unique = true)
    ]
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "username", nullable = false, unique = true, length = 50)
    var username: String,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Column(name = "full_name", length = 100)
    var fullName: String? = null,

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    var profilePictureUrl: String? = null,

    @Column(name = "bio", length = 500)
    var bio: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "last_seen_at", columnDefinition = "TIMESTAMPTZ")
    var lastSeenAt: java.time.OffsetDateTime? = null
) : AuditableEntity()
