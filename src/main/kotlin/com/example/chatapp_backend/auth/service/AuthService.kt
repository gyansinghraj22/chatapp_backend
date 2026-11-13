package com.example.chatapp_backend.auth.service

import com.example.chatapp_backend.auth.dto.AuthResponse
import com.example.chatapp_backend.auth.dto.LoginRequest
import com.example.chatapp_backend.auth.dto.RegisterRequest
import com.example.chatapp_backend.auth.dto.UserProfile
import com.example.chatapp_backend.auth.security.JwtProperties
import com.example.chatapp_backend.auth.security.JwtService
import com.example.chatapp_backend.auth.security.RefreshTokenCodec
import com.example.chatapp_backend.persistence.entity.RefreshTokenEntity
import com.example.chatapp_backend.persistence.entity.UserEntity
import com.example.chatapp_backend.persistence.repository.RefreshTokenRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val username = request.username.trim()
        val email = request.email.trim().lowercase()
        if (users.findByUsernameIgnoreCase(username) != null || users.findByEmailIgnoreCase(email) != null) {
            throw AccountAlreadyExistsException("Email or username is already registered")
        }
        val user = users.save(
            UserEntity(
                username = username,
                email = email,
                passwordHash = passwordEncoder.encode(request.password),
                fullName = request.fullName?.trim()?.takeIf(String::isNotEmpty)
            )
        )
        return issueTokens(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val identifier = request.identifier.trim()
        val user = users.findByEmailIgnoreCase(identifier) ?: users.findByUsernameIgnoreCase(identifier)
        if (user == null || !user.isActive || user.passwordHash == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BadCredentialsException("Invalid credentials")
        }
        return issueTokens(user)
    }

    @Transactional
    fun refresh(rawToken: String): AuthResponse {
        val token = refreshTokens.findByTokenHashForUpdate(RefreshTokenCodec.hash(rawToken))
            ?: throw BadCredentialsException("Invalid refresh token")
        if (token.revokedAt != null || !token.expiresAt.isAfter(Instant.now()) || token.user?.isActive != true) {
            throw BadCredentialsException("Refresh token is expired or revoked")
        }
        token.revokedAt = Instant.now()
        refreshTokens.save(token)
        return issueTokens(token.user!!)
    }

    @Transactional
    fun logout(rawToken: String) {
        val token = refreshTokens.findByTokenHashForUpdate(RefreshTokenCodec.hash(rawToken)) ?: return
        if (token.revokedAt == null) {
            token.revokedAt = Instant.now()
            refreshTokens.save(token)
        }
    }

    private fun issueTokens(user: UserEntity): AuthResponse {
        val userId = user.id
        val rawRefreshToken = RefreshTokenCodec.issue()
        val refreshEntity = RefreshTokenEntity(
            user = user,
            tokenHash = RefreshTokenCodec.hash(rawRefreshToken),
            expiresAt = Instant.now().plus(jwtProperties.refreshTokenDays, ChronoUnit.DAYS)
        )
        refreshTokens.save(refreshEntity)
        return AuthResponse(
            accessToken = jwtService.createAccessToken(userId, user.username),
            refreshToken = rawRefreshToken,
            expiresIn = jwtService.accessTokenLifetimeSeconds(),
            user = UserProfile(userId.toString(), user.username, user.email, user.fullName)
        )
    }
}

class AccountAlreadyExistsException(message: String) : RuntimeException(message)
