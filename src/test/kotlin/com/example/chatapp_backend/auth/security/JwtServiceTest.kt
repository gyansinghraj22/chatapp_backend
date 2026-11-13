package com.example.chatapp_backend.auth.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.UUID

class JwtServiceTest {
    @Test
    fun `signed access token returns its subject`() {
        val props = JwtProperties(secretBase64 = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }))
        val service = JwtService(props)
        val userId = UUID.randomUUID()
        assertEquals(userId, service.parseSubject(service.createAccessToken(userId, "user")))
    }

    @Test
    fun `rejects secrets shorter than 256 bits`() {
        val props = JwtProperties(secretBase64 = Base64.getEncoder().encodeToString(ByteArray(16)))
        assertThrows(IllegalArgumentException::class.java) { JwtService(props) }
    }
}
