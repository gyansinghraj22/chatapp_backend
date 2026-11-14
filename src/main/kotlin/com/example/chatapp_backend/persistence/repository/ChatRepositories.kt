package com.example.chatapp_backend.persistence.repository

import com.example.chatapp_backend.persistence.entity.BlockedUserEntity
import com.example.chatapp_backend.persistence.entity.ChatParticipantEntity
import com.example.chatapp_backend.persistence.entity.ChatRoomEntity
import com.example.chatapp_backend.persistence.entity.DeviceTokenEntity
import com.example.chatapp_backend.persistence.entity.MessageAttachmentEntity
import com.example.chatapp_backend.persistence.entity.MessageEntity
import com.example.chatapp_backend.persistence.entity.MessageReactionEntity
import com.example.chatapp_backend.persistence.entity.MessageReceiptEntity
import com.example.chatapp_backend.persistence.entity.MessageReportEntity
import com.example.chatapp_backend.persistence.entity.NotificationDispatchStatus
import com.example.chatapp_backend.persistence.entity.NotificationEntity
import com.example.chatapp_backend.persistence.entity.NotificationOutboxEntity
import com.example.chatapp_backend.persistence.entity.RefreshTokenEntity
import com.example.chatapp_backend.persistence.entity.UserEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmailIgnoreCase(email: String): UserEntity?
    fun findByUsernameIgnoreCase(username: String): UserEntity?
}

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenEntity token join fetch token.user where token.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): RefreshTokenEntity?

    fun findAllByUser_Id(userId: UUID): List<RefreshTokenEntity>
}

interface ChatRoomRepository : JpaRepository<ChatRoomEntity, UUID> {
    fun findByDirectChatKey(directChatKey: String): ChatRoomEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from ChatRoomEntity room where room.id = :roomId")
    fun findByIdForUpdate(@Param("roomId") roomId: UUID): ChatRoomEntity?

    @Modifying
    @Query(
        value = """
            INSERT INTO chat_rooms (id, name, room_type, created_by, direct_chat_key, created_at, updated_at)
            VALUES (:roomId, NULL, 'PRIVATE', :creatorId, :directKey, now(), now())
            ON CONFLICT (direct_chat_key) WHERE direct_chat_key IS NOT NULL DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertPrivateRoomIfAbsent(
        @Param("roomId") roomId: UUID,
        @Param("creatorId") creatorId: UUID,
        @Param("directKey") directKey: String
    ): Int
}

interface ChatParticipantRepository : JpaRepository<ChatParticipantEntity, UUID> {
    fun findByRoom_IdAndUser_IdAndLeftAtIsNull(roomId: UUID, userId: UUID): ChatParticipantEntity?
    fun findAllByUser_IdAndLeftAtIsNullOrderByJoinedAtDesc(userId: UUID): List<ChatParticipantEntity>
    fun findAllByUser_IdAndLeftAtIsNullOrderByJoinedAtDesc(userId: UUID, pageable: Pageable): Page<ChatParticipantEntity>
    fun findAllByRoom_IdAndLeftAtIsNull(roomId: UUID): List<ChatParticipantEntity>
    fun findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId: UUID): List<ChatParticipantEntity>
    fun findAllByRoom_IdInAndLeftAtIsNullOrderByJoinedAtAsc(roomIds: Collection<UUID>): List<ChatParticipantEntity>
    fun existsByRoom_IdAndUser_IdAndLeftAtIsNull(roomId: UUID, userId: UUID): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select participant from ChatParticipantEntity participant where participant.room.id = :roomId and participant.leftAt is null order by participant.joinedAt asc")
    fun findActiveForRoomForUpdate(@Param("roomId") roomId: UUID): List<ChatParticipantEntity>

    @Modifying
    @Query(
        value = """
            INSERT INTO chat_participants (id, room_id, user_id, role, joined_at, is_muted, is_archived, created_at, updated_at)
            VALUES (:id, :roomId, :userId, 'MEMBER', now(), false, false, now(), now())
            ON CONFLICT (room_id, user_id) WHERE left_at IS NULL DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertActiveMemberIfAbsent(
        @Param("id") id: UUID,
        @Param("roomId") roomId: UUID,
        @Param("userId") userId: UUID
    ): Int
}

interface MessageRepository : JpaRepository<MessageEntity, UUID> {
    fun findBySender_IdAndClientMessageId(senderId: UUID, clientMessageId: UUID): MessageEntity?
    fun findByIdAndRoom_Id(id: UUID, roomId: UUID): MessageEntity?

    @Query("select m from MessageEntity m where m.room.id = :roomId and m.deletedAt is null order by m.createdAt desc, m.id desc")
    fun findLatest(@Param("roomId") roomId: UUID, pageable: Pageable): Slice<MessageEntity>

    @Query("""
        select m from MessageEntity m where m.room.id = :roomId and m.deletedAt is null
          and (m.createdAt < :beforeCreatedAt or (m.createdAt = :beforeCreatedAt and m.id < :beforeMessageId))
        order by m.createdAt desc, m.id desc
    """)
    fun findHistoryBefore(@Param("roomId") roomId: UUID, @Param("beforeCreatedAt") beforeCreatedAt: Instant,
                          @Param("beforeMessageId") beforeMessageId: UUID, pageable: Pageable): Slice<MessageEntity>

    @Query("""
        select m from MessageEntity m where m.room.id = :roomId and m.deletedAt is null
          and (m.createdAt > :afterCreatedAt or (m.createdAt = :afterCreatedAt and m.id > :afterMessageId))
        order by m.createdAt asc, m.id asc
    """)
    fun findHistoryAfter(@Param("roomId") roomId: UUID, @Param("afterCreatedAt") afterCreatedAt: Instant,
                         @Param("afterMessageId") afterMessageId: UUID, pageable: Pageable): Slice<MessageEntity>

    @Query("""
        select m from MessageEntity m where m.room.id = :roomId and m.deletedAt is null
          and lower(m.content) like lower(concat('%', :query, '%'))
        order by m.createdAt desc, m.id desc
    """)
    fun searchInRoom(@Param("roomId") roomId: UUID, @Param("query") query: String, pageable: Pageable): Slice<MessageEntity>

    @Query(value = """
        select count(*) from messages m
        join chat_participants p on p.room_id = m.room_id and p.user_id = :userId and p.left_at is null
        left join messages cursor_message on cursor_message.id = p.last_read_message_id
        where m.room_id = :roomId and m.sender_id <> :userId and m.deleted_at is null
          and (p.last_read_message_id is null or (m.created_at, m.id) > (cursor_message.created_at, cursor_message.id))
    """, nativeQuery = true)
    fun countUnreadMessages(@Param("roomId") roomId: UUID, @Param("userId") userId: UUID): Long
}

interface MessageAttachmentRepository : JpaRepository<MessageAttachmentEntity, UUID> {
    fun findAllByMessage_IdOrderByCreatedAtAsc(messageId: UUID): List<MessageAttachmentEntity>
}

interface MessageReactionRepository : JpaRepository<MessageReactionEntity, UUID> {
    fun findAllByMessage_IdOrderByCreatedAtAsc(messageId: UUID): List<MessageReactionEntity>
    fun findByMessage_IdAndUser_IdAndReaction(messageId: UUID, userId: UUID, reaction: String): MessageReactionEntity?
    fun deleteByMessage_IdAndUser_IdAndReaction(messageId: UUID, userId: UUID, reaction: String): Long
}

interface MessageReceiptRepository : JpaRepository<MessageReceiptEntity, UUID> {
    fun findByMessage_IdAndRecipient_Id(messageId: UUID, recipientId: UUID): MessageReceiptEntity?
    fun findAllByMessage_Id(messageId: UUID): List<MessageReceiptEntity>
    fun findAllByRecipient_IdAndMessage_Room_Id(recipientId: UUID, roomId: UUID): List<MessageReceiptEntity>
}

interface BlockedUserRepository : JpaRepository<BlockedUserEntity, UUID> {
    fun existsByBlocker_IdAndBlockedUser_Id(blockerId: UUID, blockedUserId: UUID): Boolean
    fun findAllByBlocker_Id(blockerId: UUID): List<BlockedUserEntity>
}

interface MessageReportRepository : JpaRepository<MessageReportEntity, UUID> {
    fun findAllByStatusOrderByCreatedAtAsc(status: com.example.chatapp_backend.persistence.entity.MessageReportStatus): List<MessageReportEntity>
    fun existsByMessage_IdAndReporter_Id(messageId: UUID, reporterId: UUID): Boolean
}

interface NotificationRepository : JpaRepository<NotificationEntity, UUID> {
    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): List<NotificationEntity>
    fun countByUser_IdAndIsReadFalse(userId: UUID): Long
}

interface DeviceTokenRepository : JpaRepository<DeviceTokenEntity, UUID> {
    fun findAllByUser_IdAndIsActiveTrue(userId: UUID): List<DeviceTokenEntity>
    fun findByTokenHash(tokenHash: String): DeviceTokenEntity?
}

interface NotificationOutboxRepository : JpaRepository<NotificationOutboxEntity, UUID> {
    fun findAllByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
        statuses: Collection<NotificationDispatchStatus>, nextAttemptAt: Instant, pageable: Pageable
    ): List<NotificationOutboxEntity>
}
