package com.example.chatapp_backend.persistence.repository

import com.example.chatapp_backend.persistence.entity.OAuthIdentityEntity
import com.example.chatapp_backend.persistence.entity.PasswordResetTokenEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface OAuthIdentityRepository : JpaRepository<OAuthIdentityEntity, UUID> {
    fun findByProviderAndSubject(provider: String, subject: String): OAuthIdentityEntity?
    fun existsByUser_IdAndProvider(userId: UUID, provider: String): Boolean
}

interface PasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetTokenEntity token join fetch token.user where token.tokenHash = :hash")
    fun findByTokenHashForUpdate(@Param("hash") hash: String): PasswordResetTokenEntity?

    @Modifying
    @Query("update PasswordResetTokenEntity token set token.usedAt = :now where token.user.id = :userId and token.usedAt is null")
    fun invalidateUnusedForUser(@Param("userId") userId: UUID, @Param("now") now: Instant): Int
}
