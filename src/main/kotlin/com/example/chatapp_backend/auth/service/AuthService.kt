package com.example.chatapp_backend.auth.service

import com.example.chatapp_backend.auth.dto.AuthResponse
import com.example.chatapp_backend.auth.dto.ChangePasswordRequest
import com.example.chatapp_backend.auth.dto.ForgotPasswordRequest
import com.example.chatapp_backend.auth.dto.GoogleSignInRequest
import com.example.chatapp_backend.auth.dto.LoginRequest
import com.example.chatapp_backend.auth.dto.RegisterRequest
import com.example.chatapp_backend.auth.dto.ResetPasswordRequest
import com.example.chatapp_backend.auth.dto.UpdateProfileRequest
import com.example.chatapp_backend.auth.dto.UserProfile
import com.example.chatapp_backend.auth.security.GoogleIdTokenVerifierService
import com.example.chatapp_backend.auth.security.JwtProperties
import com.example.chatapp_backend.auth.security.JwtService
import com.example.chatapp_backend.auth.security.RefreshTokenCodec
import com.example.chatapp_backend.persistence.entity.OAuthIdentityEntity
import com.example.chatapp_backend.persistence.entity.PasswordResetTokenEntity
import com.example.chatapp_backend.persistence.entity.RefreshTokenEntity
import com.example.chatapp_backend.persistence.entity.UserEntity
import com.example.chatapp_backend.persistence.repository.OAuthIdentityRepository
import com.example.chatapp_backend.persistence.repository.PasswordResetTokenRepository
import com.example.chatapp_backend.persistence.repository.RefreshTokenRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

@Service
class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val resetTokens: PasswordResetTokenRepository,
    private val identities: OAuthIdentityRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val googleVerifier: GoogleIdTokenVerifierService,
    private val mailSender: JavaMailSender,
    @Value("\${app.auth.password-reset-url}") private val passwordResetUrl: String,
    @Value("\${app.auth.password-reset-minutes:30}") private val resetMinutes: Long,
    @Value("\${app.auth.password-reset-enabled:false}") private val resetEmailEnabled: Boolean,
    @Value("\${app.auth.mail-from:no-reply@localhost}") private val mailFrom: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val username = request.username.trim()
        val email = request.email.trim().lowercase()
        ensureUnique(username, email)
        val user = users.saveAndFlush(UserEntity(
            username = username,
            email = email,
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName?.trim()?.takeIf(String::isNotEmpty)
        ))
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
    fun googleSignIn(request: GoogleSignInRequest): AuthResponse {
        val google = googleVerifier.verify(request.idToken)
        val existingIdentity = identities.findByProviderAndSubject("GOOGLE", google.subject)
        val user = if (existingIdentity != null) {
            existingIdentity.user?.takeIf { it.isActive } ?: throw BadCredentialsException("Account is disabled")
        } else {
            val byEmail = users.findByEmailIgnoreCase(google.email)
            val account = if (byEmail != null) {
                if (!byEmail.isActive) throw BadCredentialsException("Account is disabled")
                byEmail
            } else {
                val username = uniqueUsername(google.email.substringBefore('@'))
                users.saveAndFlush(UserEntity(
                    username = username,
                    email = google.email,
                    passwordHash = null,
                    fullName = google.name?.take(100),
                    profilePictureUrl = google.picture
                ))
            }
            if (!identities.existsByUser_IdAndProvider(account.id, "GOOGLE")) {
                identities.saveAndFlush(OAuthIdentityEntity(user = account, provider = "GOOGLE", subject = google.subject))
            }
            account
        }
        return issueTokens(user)
    }

    /** Always returns the same response, whether or not an eligible account exists. */
    @Transactional
    fun forgotPassword(request: ForgotPasswordRequest) {
        if (!resetEmailEnabled) return
        val user = users.findByEmailIgnoreCase(request.email.trim())?.takeIf { it.isActive && it.passwordHash != null } ?: return
        resetTokens.invalidateUnusedForUser(user.id, Instant.now())
        val raw = newResetToken()
        resetTokens.save(PasswordResetTokenEntity(
            user = user,
            tokenHash = sha256(raw),
            expiresAt = Instant.now().plus(resetMinutes.coerceIn(5, 120), ChronoUnit.MINUTES)
        ))
        val encoded = URLEncoder.encode(raw, StandardCharsets.UTF_8)
        val url = "$passwordResetUrl${if (passwordResetUrl.contains('?')) '&' else '?'}token=$encoded"
        try {
            val mimeMessage: MimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name()).apply {
                setFrom(mailFrom)
                setTo(user.email)
                setSubject("Reset your ChatApp password")
                setText("Use this one-time link to reset your password. It expires soon: $url", false)
            }
            mailSender.send(mimeMessage)
        } catch (ex: Exception) {
            logger.warn("Password reset email delivery failed for account {}", user.id)
        }
    }

    @Transactional
    fun resetPassword(request: ResetPasswordRequest) {
        val token = resetTokens.findByTokenHashForUpdate(sha256(request.token))
            ?: throw BadCredentialsException("Password reset token is invalid or expired")
        val now = Instant.now()
        if (token.usedAt != null || !token.expiresAt.isAfter(now)) {
            throw BadCredentialsException("Password reset token is invalid or expired")
        }
        val user = token.user?.takeIf { it.isActive }
            ?: throw BadCredentialsException("Password reset token is invalid or expired")
        token.usedAt = now
        resetTokens.save(token)
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        users.save(user)
        refreshTokens.findAllByUser_Id(user.id).forEach { refresh ->
            if (refresh.revokedAt == null) refresh.revokedAt = now
        }
    }

    @Transactional
    fun changePassword(userId: UUID, request: ChangePasswordRequest) {
        val user = users.findById(userId).filter { it.isActive }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val currentHash = user.passwordHash ?: throw BadCredentialsException("Password authentication is not enabled for this account")
        if (!passwordEncoder.matches(request.currentPassword, currentHash)) throw BadCredentialsException("Current password is incorrect")
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        users.save(user)
        val now = Instant.now()
        refreshTokens.findAllByUser_Id(userId).forEach { if (it.revokedAt == null) it.revokedAt = now }
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UpdateProfileRequest): UserProfile {
        val user = users.findById(userId).filter { it.isActive }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        request.username?.trim()?.takeIf(String::isNotEmpty)?.let { username ->
            val existing = users.findByUsernameIgnoreCase(username)
            if (existing != null && existing.id != userId) throw AccountAlreadyExistsException("Username is already in use")
            user.username = username
        }
        request.fullName?.let { user.fullName = it.trim().takeIf(String::isNotEmpty) }
        request.bio?.let { user.bio = it.trim().takeIf(String::isNotEmpty) }
        request.profilePictureUrl?.let { user.profilePictureUrl = it.trim().takeIf(String::isNotEmpty) }
        return profile(users.save(user))
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

    private fun ensureUnique(username: String, email: String) {
        if (username.isBlank() || email.isBlank()) throw IllegalArgumentException("Username and email are required")
        if (users.findByUsernameIgnoreCase(username) != null || users.findByEmailIgnoreCase(email) != null) {
            throw AccountAlreadyExistsException("Email or username is already registered")
        }
    }

    private fun uniqueUsername(candidate: String): String {
        val normalized = candidate.lowercase().replace(Regex("[^a-z0-9._-]"), "").take(40).ifBlank { "user" }
        if (users.findByUsernameIgnoreCase(normalized) == null) return normalized
        repeat(10) {
            val alternative = "${normalized.take(38)}-${UUID.randomUUID().toString().take(6)}"
            if (users.findByUsernameIgnoreCase(alternative) == null) return alternative
        }
        throw IllegalStateException("Could not allocate a unique username")
    }

    private fun issueTokens(user: UserEntity): AuthResponse {
        val userId = user.id
        val rawRefreshToken = RefreshTokenCodec.issue()
        refreshTokens.save(RefreshTokenEntity(
            user = user,
            tokenHash = RefreshTokenCodec.hash(rawRefreshToken),
            expiresAt = Instant.now().plus(jwtProperties.refreshTokenDays, ChronoUnit.DAYS)
        ))
        return AuthResponse(
            accessToken = jwtService.createAccessToken(userId, user.username),
            refreshToken = rawRefreshToken,
            expiresIn = jwtService.accessTokenLifetimeSeconds(),
            user = profile(user)
        )
    }

    private fun profile(user: UserEntity) = UserProfile(
        id = user.id.toString(), username = user.username, email = user.email,
        fullName = user.fullName, profilePictureUrl = user.profilePictureUrl, bio = user.bio
    )

    private fun newResetToken(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

class AccountAlreadyExistsException(message: String) : RuntimeException(message)
