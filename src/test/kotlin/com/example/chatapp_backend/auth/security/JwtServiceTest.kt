package com.example.chatapp_backend.auth.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.UUID

class JwtServiceTest {
    @Test
    fun `signed access token contains expected subject`() {
        val properties = JwtProperties(secretBase64 = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }))
        val service = JwtService(properties)
        val userId = UUID.randomUUID()
        assertEquals(userId, service.parseSubject(service.createAccessToken(userId, "alice")))
    }

    @Test
    fun `wrong signing key cannot parse token`() {
        val first = JwtService(JwtProperties(secretBase64 = Base64.getEncoder().encodeToString(ByteArray(32) { 1 })))
        val second = JwtService(JwtProperties(secretBase64 = Base64.getEncoder().encodeToString(ByteArray(32) { 2 })))
        assertThrows(Exception::class.java) { second.parseSubject(first.createAccessToken(UUID.randomUUID(), "alice")) }
    }

    @Test
    fun `short secret is rejected`() {
        val properties = JwtProperties(secretBase64 = Base64.getEncoder().encodeToString(ByteArray(16)))
        assertThrows(IllegalArgumentException::class.java) { JwtService(properties) }
    }
}
