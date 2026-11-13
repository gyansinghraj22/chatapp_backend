package com.example.chatapp_backend.chat.repository

import com.example.chatapp_backend.auth.domain.User
import com.example.chatapp_backend.chat.domain.*
import com.example.chatapp_backend.chat.enums.ChatParticipantRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface ChatRoomRepository : JpaRepository<ChatRoom, UUID> {
    fun findByDirectChatKey(directChatKey: String): Optional<ChatRoom>
    fun findByCreatedBy(createdBy: User): List<ChatRoom>
}

interface ChatParticipantRepository : JpaRepository<ChatParticipant, UUID> {
    fun findByRoomIdAndUserId(roomId: UUID, userId: UUID): Optional<ChatParticipant>
    fun findByUserIdAndRoomId(userId: UUID, roomId: UUID): Optional<ChatParticipant>
    fun findByRoomId(roomId: UUID): List<ChatParticipant>
    fun findByUserId(userId: UUID): List<ChatParticipant>
    fun findByRoomIdAndRole(roomId: UUID, role: ChatParticipantRole): List<ChatParticipant>
}

interface MessageRepository : JpaRepository<Message, UUID> {
    fun findByRoomIdOrderByCreatedAtDesc(roomId: UUID): List<Message>
    fun findBySenderIdAndClientMessageId(senderId: UUID, clientMessageId: UUID): Optional<Message>
}

interface MessageAttachmentRepository : JpaRepository<MessageAttachment, UUID> {
    fun findByMessageId(messageId: UUID): List<MessageAttachment>
}

interface MessageReactionRepository : JpaRepository<MessageReaction, UUID> {
    fun findByMessageId(messageId: UUID): List<MessageReaction>
}

interface MessageReceiptRepository : JpaRepository<MessageReceipt, UUID> {
    fun findByMessageIdAndRecipientId(messageId: UUID, recipientId: UUID): Optional<MessageReceipt>
    fun findByRecipientId(recipientId: UUID): List<MessageReceipt>
}

interface BlockedUserRepository : JpaRepository<BlockedUser, UUID> {
    fun findByBlockerIdAndBlockedUserId(blockerId: UUID, blockedUserId: UUID): Optional<BlockedUser>
    fun findByBlockerId(blockerId: UUID): List<BlockedUser>
}

interface MessageReportRepository : JpaRepository<MessageReport, UUID> {
    fun findByReporterId(reporterId: UUID): List<MessageReport>
    fun findByMessageId(messageId: UUID): List<MessageReport>
}

interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>
    fun findByUserIdAndIsRead(userId: UUID, isRead: Boolean): List<Notification>
}

interface DeviceTokenRepository : JpaRepository<DeviceToken, UUID> {
    fun findByUserId(userId: UUID): List<DeviceToken>
    fun findByToken(token: String): Optional<DeviceToken>
}

interface NotificationOutboxRepository : JpaRepository<NotificationOutbox, UUID> {
    fun findByStatus(status: String): List<NotificationOutbox>
}
