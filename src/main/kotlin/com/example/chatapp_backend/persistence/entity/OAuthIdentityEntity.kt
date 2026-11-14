package com.example.chatapp_backend.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "oauth_identities",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_oauth_provider_subject", columnNames = ["provider", "subject"]),
        UniqueConstraint(name = "uk_oauth_user_provider", columnNames = ["user_id", "provider"])
    ]
)
class OAuthIdentityEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_oauth_identity_user"))
    var user: UserEntity? = null,

    @Column(name = "provider", nullable = false, length = 20)
    var provider: String = "GOOGLE",

    @Column(name = "subject", nullable = false, length = 255)
    var subject: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
) : BaseEntity()
