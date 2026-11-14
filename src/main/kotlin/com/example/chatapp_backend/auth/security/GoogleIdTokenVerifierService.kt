package com.example.chatapp_backend.auth.security

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GoogleIdTokenVerifierService(@Value("\${app.auth.google-client-id:}") private val clientId: String) {
    private val verifier by lazy {
        require(clientId.isNotBlank()) { "GOOGLE_CLIENT_ID must be configured to enable Google Sign-In" }
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance())
            .setAudience(listOf(clientId))
            .build()
    }

    fun verify(rawToken: String): VerifiedGoogleIdentity {
        val idToken = try { verifier.verify(rawToken) } catch (ex: Exception) {
            throw InvalidGoogleTokenException("Google ID token is invalid", ex)
        } ?: throw InvalidGoogleTokenException("Google ID token is invalid")
        val payload = idToken.payload
        val email = payload.email ?: throw InvalidGoogleTokenException("Google account has no email")
        if (payload.emailVerified != true) throw InvalidGoogleTokenException("Google email is not verified")
        val subject = payload.subject ?: throw InvalidGoogleTokenException("Google token has no subject")
        return VerifiedGoogleIdentity(
            subject = subject,
            email = email.trim().lowercase(),
            name = payload["name"] as? String,
            picture = payload["picture"] as? String
        )
    }
}

data class VerifiedGoogleIdentity(val subject: String, val email: String, val name: String?, val picture: String?)
class InvalidGoogleTokenException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
