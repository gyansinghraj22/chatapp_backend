package com.example.chatapp_backend.media.dto

import java.time.Instant
import java.util.UUID

data class MediaUploadResponse(
    val id: UUID,
    val fileName: String,
    val contentType: String,
    val size: Long,
    val sha256: String,
    val status: String,
    val downloadUrl: String,
    val expiresAt: Instant
)
