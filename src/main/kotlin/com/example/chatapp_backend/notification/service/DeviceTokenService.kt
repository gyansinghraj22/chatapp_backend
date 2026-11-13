package com.example.chatapp_backend.notification.service

import com.example.chatapp_backend.notification.dto.RegisterDeviceRequest
import com.example.chatapp_backend.persistence.entity.DevicePlatform
import com.example.chatapp_backend.persistence.entity.DeviceTokenEntity
import com.example.chatapp_backend.persistence.repository.DeviceTokenRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class DeviceTokenService(
    private val tokens: DeviceTokenRepository,
    private val users: UserRepository,
    @Value("\${app.notifications.token-encryption-key:}") private val encryptionKeyBase64: String
) {
    private val random = SecureRandom()

    @Transactional
    fun register(userId: UUID, request: RegisterDeviceRequest) {
        val user = users.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val platform = runCatching { DevicePlatform.valueOf(request.platform.uppercase()) }
            .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Platform must be ANDROID, IOS, or WEB") }
        val hash = sha256(request.token)
        val existing = tokens.findByTokenHash(hash)
        if (existing != null) {
            existing.user = user
            existing.tokenCiphertext = encrypt(request.token)
            existing.platform = platform
            existing.isActive = true
            tokens.save(existing)
        } else {
            tokens.save(DeviceTokenEntity(user = user, tokenHash = hash, tokenCiphertext = encrypt(request.token), platform = platform))
        }
    }

    @Transactional
    fun unregister(userId: UUID, rawToken: String) {
        val existing = tokens.findByTokenHash(sha256(rawToken)) ?: return
        if (existing.user?.id == userId) {
            existing.isActive = false
            tokens.save(existing)
        }
    }

    fun decrypt(encrypted: String): String {
        val packed = Base64.getDecoder().decode(encrypted)
        val buffer = ByteBuffer.wrap(packed)
        val iv = ByteArray(12).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes(), "AES"), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun encrypt(raw: String): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes(), "AES"), GCMParameterSpec(128, iv))
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(raw.toByteArray(Charsets.UTF_8)))
    }

    private fun keyBytes(): ByteArray {
        val key = runCatching { Base64.getDecoder().decode(encryptionKeyBase64) }
            .getOrElse { throw IllegalStateException("FCM_TOKEN_ENCRYPTION_KEY must be Base64 encoded") }
        require(key.size == 32) { "FCM_TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes" }
        return key
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
