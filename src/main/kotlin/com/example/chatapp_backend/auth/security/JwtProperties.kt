package com.example.chatapp_backend.auth.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Base64

@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    var secretBase64: String = "",
    var accessTokenSeconds: Long = 900,
    var refreshTokenDays: Long = 30
) {
    fun decodedSecret(): ByteArray {
        require(secretBase64.isNotBlank()) { "JWT_SECRET must be configured as a Base64-encoded secret" }
        val decoded = try {
            Base64.getDecoder().decode(secretBase64)
        } catch (ex: IllegalArgumentException) {
            throw IllegalStateException("JWT_SECRET must be valid Base64", ex)
        }
        require(decoded.size >= 32) { "JWT_SECRET must decode to at least 32 bytes" }
        require(accessTokenSeconds in 60..86400) { "JWT access-token lifetime must be between 60 and 86400 seconds" }
        require(refreshTokenDays in 1..365) { "JWT refresh-token lifetime must be between 1 and 365 days" }
        return decoded
    }
}
