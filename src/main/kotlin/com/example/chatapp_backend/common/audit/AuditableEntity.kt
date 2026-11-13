package com.example.chatapp_backend.common.audit

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@MappedSuperclass
abstract class AuditableEntity {
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    var createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    var updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

    @PrePersist
    fun onCreate() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    }
}
