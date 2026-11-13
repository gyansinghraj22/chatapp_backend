package com.example.chatapp_backend.auth.controller

import com.example.chatapp_backend.auth.dto.UserProfile
import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/users")
class AuthenticatedUserController(private val users: UserRepository) {
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: AuthenticatedUser): UserProfile {
        val user = users.findById(principal.id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        return UserProfile(user.id.toString(), user.username, user.email, user.fullName)
    }
}
