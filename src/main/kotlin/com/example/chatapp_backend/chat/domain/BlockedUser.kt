package com.example.chatapp_backend.chat.domain

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.common.audit.AuditableEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "blocked_users",
    indexes = [Index(name = "idx_blocked_users_blocker", columnList = "blocker_id")]
)
class BlockedUser(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false, foreignKey = ForeignKey(name = "fk_blocked_users_blocker"))
    var blocker: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id", nullable = false, foreignKey = ForeignKey(name = "fk_blocked_users_blocked_user"))
    var blockedUser: User,
) : AuditableEntity()
