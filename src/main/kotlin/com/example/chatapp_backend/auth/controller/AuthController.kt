package com.example.chatapp_backend.auth.controller

import com.example.chatapp_backend.auth.dto.*
import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse = authService.login(request)

    @PostMapping("/google")
    fun google(@Valid @RequestBody request: GoogleSignInRequest): AuthResponse = authService.googleSignIn(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): AuthResponse = authService.refresh(request.refreshToken)

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<Void> {
        authService.logout(request.refreshToken)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): MessageResponse {
        authService.forgotPassword(request)
        return MessageResponse("If an eligible account exists, password reset instructions will be sent.")
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest) = authService.resetPassword(request)

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: ChangePasswordRequest
    ) = authService.changePassword(principal.id, request)
}
