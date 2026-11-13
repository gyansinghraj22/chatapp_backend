package com.example.chatapp_backend.chat.service

import com.example.chatapp_backend.chat.dto.MessageHistoryResponse
import com.example.chatapp_backend.chat.dto.MessageResponse
import com.example.chatapp_backend.chat.dto.SendMessageRequest
import com.example.chatapp_backend.persistence.entity.MessageEntity
import com.example.chatapp_backend.persistence.entity.MessageType
import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
import com.example.chatapp_backend.persistence.repository.ChatRoomRepository
import com.example.chatapp_backend.persistence.repository.MessageRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class MessageService(
    private val messages: MessageRepository,
    private val participants: ChatParticipantRepository,
    private val rooms: ChatRoomRepository,
    private val users: UserRepository
) {
    companion object { private const val MAX_PAGE_SIZE = 100 }

    /** Transaction completes before the controller emits to the broker. */
    @Transactional
    fun send(senderId: UUID, roomId: UUID, request: SendMessageRequest): MessageResponse {
        val sender = users.findById(senderId).filter { it.isActive }
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active account required") }
        val room = rooms.findById(roomId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found") }
        if (!participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, senderId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Active room membership is required to send messages")
        }

        messages.findBySender_IdAndClientMessageId(senderId, request.clientMessageId)?.let { existing ->
            if (existing.room?.id != roomId) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Client message ID was already used in another room")
            }
            return response(existing)
        }

        val text = request.content?.trim()?.takeIf(String::isNotEmpty)
        if (request.messageType == MessageType.TEXT && text == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Text messages require non-empty content")
        }
        if (request.messageType == MessageType.SYSTEM) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "System messages cannot be sent by clients")
        }
        if (text == null && request.messageType == MessageType.TEXT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required")
        }

        val reply = request.replyToMessageId?.let { replyId ->
            messages.findByIdAndRoom_Id(replyId, roomId)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply target must be a message in this room")
        }
        val saved = messages.saveAndFlush(
            MessageEntity(
                clientMessageId = request.clientMessageId,
                room = room,
                sender = sender,
                content = text,
                messageType = request.messageType,
                replyToMessage = reply
            )
        )
        return response(saved)
    }

    @Transactional(readOnly = true)
    fun history(
        userId: UUID,
        roomId: UUID,
        beforeCreatedAt: Instant?,
        beforeMessageId: UUID?,
        size: Int
    ): MessageHistoryResponse {
        if (size !in 1..MAX_PAGE_SIZE) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and $MAX_PAGE_SIZE")
        if ((beforeCreatedAt == null) != (beforeMessageId == null)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Both beforeCreatedAt and beforeMessageId are required for a cursor")
        }
        rooms.findById(roomId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found") }
        if (!participants.existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId, userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Active room membership is required")
        }
        val slice = if (beforeCreatedAt == null) {
            messages.findLatest(roomId, PageRequest.of(0, size))
        } else {
            messages.findHistoryBefore(roomId, beforeCreatedAt, beforeMessageId!!, PageRequest.of(0, size))
        }
        val results = slice.content.map(::response)
        val oldest = results.lastOrNull()
        return MessageHistoryResponse(results, slice.hasNext(), oldest?.createdAt, oldest?.id)
    }

    private fun response(message: MessageEntity): MessageResponse {
        val sender = message.sender ?: throw IllegalStateException("Persisted message has no sender")
        val room = message.room ?: throw IllegalStateException("Persisted message has no room")
        return MessageResponse(
            id = message.id,
            clientMessageId = message.clientMessageId,
            roomId = room.id,
            senderId = sender.id,
            senderUsername = sender.username,
            content = if (message.deletedAt == null) message.content else null,
            messageType = message.messageType,
            replyToMessageId = message.replyToMessage?.id,
            createdAt = message.createdAt,
            editedAt = message.editedAt,
            deletedAt = message.deletedAt
        )
    }
}
