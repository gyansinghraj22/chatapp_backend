package com.example.chatapp_backend.notification.service

import com.example.chatapp_backend.persistence.entity.ChatParticipantEntity
import com.example.chatapp_backend.persistence.entity.NotificationEntity
import com.example.chatapp_backend.persistence.entity.NotificationOutboxEntity
import com.example.chatapp_backend.persistence.entity.NotificationPreferenceEntity
import com.example.chatapp_backend.persistence.entity.RoomType
import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
import com.example.chatapp_backend.persistence.repository.NotificationEntityRepository
import com.example.chatapp_backend.persistence.repository.NotificationOutboxEntityRepository
import com.example.chatapp_backend.persistence.repository.NotificationPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MessageNotificationService(
    private val participants: ChatParticipantRepository,
    private val preferences: NotificationPreferenceRepository,
    private val notifications: NotificationEntityRepository,
    private val outbox: NotificationOutboxEntityRepository
) {
    @Transactional
    fun enqueueForMessage(messageId: UUID, senderId: UUID, roomId: UUID, roomType: RoomType, senderName: String, roomName: String?, bodyText: String?) {
        val active = participants.findAllByRoom_IdAndLeftAtIsNullOrderByJoinedAtAsc(roomId)
        active.asSequence().mapNotNull(ChatParticipantEntity::user)
            .filter { it.id != senderId }
            .forEach { recipient ->
                val preference = preferences.findByUser_Id(recipient.id) ?: NotificationPreferenceEntity(user = recipient)
                if (!preference.pushEnabled) return@forEach
                if (roomType == RoomType.GROUP && !preference.groupNotificationsEnabled) return@forEach
                if (roomType == RoomType.PRIVATE && !preference.messageNotificationsEnabled) return@forEach
                val participant = active.firstOrNull { it.user?.id == recipient.id }
                if (participant?.isMuted == true || participant?.isArchived == true) return@forEach
                val title = if (roomType == RoomType.GROUP) roomName?.take(255) ?: "New group message" else senderName.take(255)
                val body = bodyText?.take(500) ?: "Sent an attachment"
                val notification = notifications.save(
                    NotificationEntity(
                        user = recipient,
                        sender = active.firstOrNull { it.user?.id == senderId }?.user,
                        room = active.firstOrNull { it.room?.id == roomId }?.room,
                        message = null,
                        title = title,
                        body = body,
                        notificationType = if (roomType == RoomType.GROUP) "GROUP_MESSAGE" else "PRIVATE_MESSAGE"
                    )
                )
                outbox.save(NotificationOutboxEntity(notification = notification))
            }
    }
}
