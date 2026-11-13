package com.example.chatapp_backend.persistence.repository

import com.example.chatapp_backend.persistence.entity.RefreshTokenEntity
import com.example.chatapp_backend.persistence.entity.UserEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmailIgnoreCase(email: String): UserEntity?
    fun findByUsernameIgnoreCase(username: String): UserEntity?
}

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenEntity token join fetch token.user where token.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): RefreshTokenEntity?

    fun findAllByUser_Id(userId: UUID): List<RefreshTokenEntity>
}
