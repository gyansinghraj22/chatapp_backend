package com.example.chatapp_backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 3, max = 50) val username: String,
    @field:NotBlank @field:Email @field:Size(max = 255) val email: String,
    @field:NotBlank @field:Size(min = 10, max = 128) val password: String,
    @field:Size(max = 100) val fullName: String? = null
)

data class LoginRequest(
    @field:NotBlank val identifier: String,
    @field:NotBlank val password: String
)

data class RefreshRequest(@field:NotBlank val refreshToken: String)

data class LogoutRequest(@field:NotBlank val refreshToken: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserProfile
)

data class UserProfile(val id: String, val username: String, val email: String, val fullName: String?)
