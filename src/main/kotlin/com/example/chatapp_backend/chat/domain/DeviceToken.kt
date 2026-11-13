package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "device_tokens",
    indexes = [Index(name = "idx_device_tokens_user_id", columnList = "user_id")],
    uniqueConstraints = [UniqueConstraint(name = "uk_device_tokens_token", columnNames = ["token"]) ]
)
class DeviceToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_device_tokens_user"))
    var user: User,

    @Column(name = "token", nullable = false, length = 255)
    var token: String,

    @Column(name = "platform", length = 20)
    var platform: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) : AuditableEntity()
