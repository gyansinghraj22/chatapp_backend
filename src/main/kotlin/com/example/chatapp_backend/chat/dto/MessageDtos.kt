package com.example.chatapp_backend.chat.dto

import com.example.chatapp_backend.persistence.entity.MessageType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SendMessageRequest(
    @field:NotNull val clientMessageId: UUID,
    @field:Size(max = 10000) val content: String? = null,
    val messageType: MessageType = MessageType.TEXT,
    val replyToMessageId: UUID? = null
)

data class MessageResponse(
    val id: UUID,
    val clientMessageId: UUID,
    val roomId: UUID,
    val senderId: UUID,
    val senderUsername: String,
    val content: String?,
    val messageType: MessageType,
    val replyToMessageId: UUID?,
    val createdAt: Instant,
    val editedAt: Instant?,
    val deletedAt: Instant?
)

data class MessageHistoryResponse(
    val messages: List<MessageResponse>,
    val hasMore: Boolean,
    val nextBeforeCreatedAt: Instant?,
    val nextBeforeMessageId: UUID?
)
