package com.example.chatapp_backend.auth.repository

import com.example.chatapp_backend.auth.domain.RefreshToken
import com.example.chatapp_backend.auth.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>
    fun findByUser(user: User): List<RefreshToken>
}

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
}
