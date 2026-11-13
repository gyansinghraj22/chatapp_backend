package com.example.chatapp_backend.auth.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object RefreshTokenCodec {
    private val secureRandom = SecureRandom()

    fun issue(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
