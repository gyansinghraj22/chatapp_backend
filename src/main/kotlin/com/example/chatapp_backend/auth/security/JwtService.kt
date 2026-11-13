package com.example.chatapp_backend.auth.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class JwtService(properties: JwtProperties) {
    private val secretKey = Keys.hmacShaKeyFor(properties.decodedSecret())
    private val accessTokenSeconds = properties.accessTokenSeconds

    fun createAccessToken(userId: UUID, username: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessTokenSeconds)))
            .signWith(secretKey)
            .compact()
    }

    fun parseSubject(token: String): UUID = UUID.fromString(
        Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).payload.subject
    )

    fun accessTokenLifetimeSeconds(): Long = accessTokenSeconds
}
