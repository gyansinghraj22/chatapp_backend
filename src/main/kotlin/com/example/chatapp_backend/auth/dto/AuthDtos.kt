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

data class LoginRequest(@field:NotBlank val identifier: String, @field:NotBlank val password: String)
data class RefreshRequest(@field:NotBlank val refreshToken: String)
data class LogoutRequest(@field:NotBlank val refreshToken: String)
data class ForgotPasswordRequest(@field:NotBlank @field:Email @field:Size(max = 255) val email: String)
data class ResetPasswordRequest(
    @field:NotBlank @field:Size(max = 256) val token: String,
    @field:NotBlank @field:Size(min = 10, max = 128) val newPassword: String
)
data class GoogleSignInRequest(@field:NotBlank @field:Size(max = 8192) val idToken: String)
data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    @field:NotBlank @field:Size(min = 10, max = 128) val newPassword: String
)
data class UpdateProfileRequest(
    @field:Size(min = 3, max = 50) val username: String? = null,
    @field:Size(max = 100) val fullName: String? = null,
    @field:Size(max = 500) val bio: String? = null,
    @field:Size(max = 2048) val profilePictureUrl: String? = null
)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserProfile
)
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String?,
    val profilePictureUrl: String? = null,
    val bio: String? = null
)
data class MessageResponse(val message: String)
