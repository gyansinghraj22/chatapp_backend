package com.example.chatapp_backend.auth/controller

import com.example.chatapp_backend.auth.dto.UserProfile
import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.auth.service.AuthService
import com.example.chatapp_backend.auth.dto.UpdateProfileRequest
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserProfileController(private val authService: AuthService) {
    @PatchMapping("/me")
    fun update(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: UpdateProfileRequest
    ): UserProfile = authService.updateProfile(principal.id, request)
}
