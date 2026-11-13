package com.example.chatapp_backend.auth.security

import java.util.UUID

data class AuthenticatedUser(val id: UUID, val username: String)
