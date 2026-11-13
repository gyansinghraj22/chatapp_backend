package com.example.chatapp_backend.auth.security

import java.security.Principal
import java.util.UUID

data class AuthenticatedUser(val id: UUID, val username: String) : Principal {
    override fun getName(): String = id.toString()
}
