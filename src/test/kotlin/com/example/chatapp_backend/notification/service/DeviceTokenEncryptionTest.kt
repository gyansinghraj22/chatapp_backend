package com.example.chatapp_backend.notification.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

class DeviceTokenEncryptionTest {
    @Test
    fun `FCM token is encrypted and can be decrypted`() {
        val repository = org.mockito.Mockito.mock(com.example.chatapp_backend.persistence.repository.DeviceTokenRepository::class.java)
        val users = org.mockito.Mockito.mock(com.example.chatapp_backend.persistence.repository.UserRepository::class.java)
        val service = DeviceTokenService(repository, users, Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }))
        val register = DeviceTokenService::class.java.getDeclaredMethod("encrypt", String::class.java).apply { isAccessible = true }
        val encrypted = register.invoke(service, "test-fcm-token") as String
        assertTrue(encrypted != "test-fcm-token")
        assertEquals("test-fcm-token", service.decrypt(encrypted))
    }
}
