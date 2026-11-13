package com.example.chatapp_backend.notification.service

import com.example.chatapp_backend.notification.firebase.PushSender
import com.example.chatapp_backend.persistence.entity.NotificationDispatchStatus
import com.example.chatapp_backend.persistence.repository.DeviceTokenRepository
import com.example.chatapp_backend.persistence.repository.NotificationOutboxRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

@Component
@ConditionalOnProperty(prefix = "app.notifications.dispatch", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class NotificationOutboxDispatcher(
    private val jdbc: JdbcTemplate,
    private val transactions: TransactionTemplate,
    private val devices: DeviceTokenRepository,
    private val deviceTokenService: DeviceTokenService,
    private val pushSender: PushSender,
    @Value("\${app.notifications.dispatch.batch-size:50}") private val batchSize: Int
) {
    @Scheduled(fixedDelayString = "\${app.notifications.dispatch.interval-ms:5000}")
    fun dispatchDue() {
        if (batchSize !in 1..500) return
        claimBatch().forEach(::dispatchOne)
    }

    private fun claimBatch(): List<OutboxItem> = transactions.execute {
        jdbc.query(
            """
            WITH picked AS (
                SELECT o.id
                FROM notification_outbox o
                WHERE (o.status IN ('PENDING', 'RETRY') AND o.next_attempt_at <= now())
                   OR (o.status = 'PROCESSING' AND o.claimed_at < now() - INTERVAL '5 minutes')
                ORDER BY o.next_attempt_at, o.created_at
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            UPDATE notification_outbox o
               SET status = 'PROCESSING', claimed_at = now(), attempts = attempts + 1, updated_at = now()
              FROM picked p, notifications n
             WHERE o.id = p.id AND n.id = o.notification_id
            RETURNING o.id, o.attempts, n.id AS notification_id, n.user_id,
                      n.title, n.body, n.notification_type, n.room_id, n.message_id
            """.trimIndent(),
            { rs, _ -> rs.toOutboxItem() },
            batchSize
        )
    } ?: emptyList()

    private fun dispatchOne(item: OutboxItem) {
        try {
            val tokenRows = devices.findAllByUser_IdAndIsActiveTrue(item.userId)
            var failures = 0
            tokenRows.forEach { device ->
                try {
                    val rawToken = deviceTokenService.decrypt(device.tokenCiphertext)
                    pushSender.send(rawToken, item.title, item.body, mapOf(
                        "type" to item.type,
                        "notificationId" to item.notificationId.toString(),
                        "roomId" to (item.roomId?.toString() ?: ""),
                        "messageId" to (item.messageId?.toString() ?: ""),
                        "click_action" to "OPEN_CHAT"
                    ))
                } catch (_: Exception) {
                    failures++
                }
            }
            if (failures == 0) {
                updateStatus(item.id, "SUCCEEDED", null, null)
            } else if (item.attempts >= MAX_ATTEMPTS) {
                updateStatus(item.id, "DEAD", "FCM delivery failed for one or more registered devices", null)
            } else {
                val delaySeconds = minOf(3600L, 15L shl (item.attempts - 1).coerceIn(0, 8))
                updateStatus(item.id, "RETRY", "FCM delivery failed for one or more registered devices", delaySeconds)
            }
        } catch (_: Exception) {
            if (item.attempts >= MAX_ATTEMPTS) {
                updateStatus(item.id, "DEAD", "Notification dispatch failed", null)
            } else {
                val delaySeconds = minOf(3600L, 15L shl (item.attempts - 1).coerceIn(0, 8))
                updateStatus(item.id, "RETRY", "Notification dispatch failed", delaySeconds)
            }
        }
    }

    private fun updateStatus(id: UUID, status: String, error: String?, delaySeconds: Long?) {
        if (delaySeconds == null) {
            jdbc.update("UPDATE notification_outbox SET status = ?, last_error = ?, claimed_at = NULL, dispatched_at = CASE WHEN ? = 'SUCCEEDED' THEN now() ELSE dispatched_at END, updated_at = now() WHERE id = ?", status, error, status, id)
        } else {
            jdbc.update("UPDATE notification_outbox SET status = ?, last_error = ?, next_attempt_at = now() + (? * INTERVAL '1 second'), claimed_at = NULL, updated_at = now() WHERE id = ?", status, error, delaySeconds, id)
        }
    }

    private fun ResultSet.toOutboxItem() = OutboxItem(
        id = getObject("id", UUID::class.java),
        attempts = getInt("attempts"),
        notificationId = getObject("notification_id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        title = getString("title"), body = getString("body"),
        type = getString("notification_type"),
        roomId = getObject("room_id", UUID::class.java),
        messageId = getObject("message_id", UUID::class.java)
    )

    private data class OutboxItem(
        val id: UUID, val attempts: Int, val notificationId: UUID, val userId: UUID,
        val title: String, val body: String?, val type: String, val roomId: UUID?, val messageId: UUID?
    )

    companion object { private const val MAX_ATTEMPTS = 8 }
}
