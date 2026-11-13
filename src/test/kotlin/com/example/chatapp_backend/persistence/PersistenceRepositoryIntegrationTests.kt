package com.example.chatapp_backend.persistence

import com.example.chatapp_backend.persistence.entity.ChatParticipantEntity
import com.example.chatapp_backend.persistence.entity.ChatRoomEntity
import com.example.chatapp_backend.persistence.entity.MessageEntity
import com.example.chatapp_backend.persistence.entity.MessageType
import com.example.chatapp_backend.persistence.entity.ParticipantRole
import com.example.chatapp_backend.persistence.entity.RoomType
import com.example.chatapp_backend.persistence.entity.UserEntity
import com.example.chatapp_backend.persistence.repository.ChatParticipantRepository
import com.example.chatapp_backend.persistence.repository.ChatRoomRepository
import com.example.chatapp_backend.persistence.repository.MessageRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceRepositoryIntegrationTests @Autowired constructor(
    private val users: UserRepository,
    private val rooms: ChatRoomRepository,
    private val participants: ChatParticipantRepository,
    private val messages: MessageRepository,
    private val jdbcTemplate: JdbcTemplate
) {
    @Test
    fun `user email uniqueness is case insensitive`() {
        users.saveAndFlush(newUser("alpha", "Alpha@example.test"))

        assertThrows(DataIntegrityViolationException::class.java) {
            users.saveAndFlush(newUser("beta", "alpha@EXAMPLE.test"))
        }
    }

    @Test
    fun `participant history allows only one active membership`() {
        val user = users.saveAndFlush(newUser("member", "member@example.test"))
        val room = newGroupRoom(user)
        val formerMembership = participants.saveAndFlush(
            ChatParticipantEntity(
                room = room,
                user = user,
                role = ParticipantRole.MEMBER,
                leftAt = Instant.now()
            )
        )

        val currentMembership = participants.saveAndFlush(
            ChatParticipantEntity(room = room, user = user, role = ParticipantRole.MEMBER)
        )

        assertThat(formerMembership.id).isNotEqualTo(currentMembership.id)
        assertThat(participants.findAllByRoom_IdAndLeftAtIsNull(room.id)).containsExactly(currentMembership)
    }

    @Test
    fun `sender client message id is idempotent`() {
        val sender = users.saveAndFlush(newUser("sender", "sender@example.test"))
        val room = newGroupRoom(sender)
        val clientMessageId = UUID.randomUUID()
        messages.saveAndFlush(
            newMessage(room, sender, clientMessageId, "first")
        )

        assertThrows(DataIntegrityViolationException::class.java) {
            messages.saveAndFlush(newMessage(room, sender, clientMessageId, "retry"))
        }
    }

    @Test
    fun `unread count uses participant cursor and excludes own messages`() {
        val recipient = users.saveAndFlush(newUser("recipient", "recipient@example.test"))
        val sender = users.saveAndFlush(newUser("sender", "sender@example.test"))
        val room = newGroupRoom(sender)
        val readCursorMessage = messages.saveAndFlush(newMessage(room, sender, UUID.randomUUID(), "read"))
        messages.saveAndFlush(newMessage(room, sender, UUID.randomUUID(), "unread"))
        messages.saveAndFlush(newMessage(room, recipient, UUID.randomUUID(), "own"))
        participants.saveAndFlush(
            ChatParticipantEntity(
                room = room,
                user = recipient,
                lastReadMessage = readCursorMessage,
                lastReadAt = readCursorMessage.createdAt
            )
        )

        assertThat(messages.countUnreadMessages(room.id, recipient.id)).isEqualTo(1)
    }

    @Test
    fun `history supports stable before and after cursors`() {
        val sender = users.saveAndFlush(newUser("cursor-user", "cursor@example.test"))
        val room = newGroupRoom(sender)
        val first = messages.saveAndFlush(newMessage(room, sender, UUID.randomUUID(), "first"))
        val second = messages.saveAndFlush(newMessage(room, sender, UUID.randomUUID(), "second"))
        val third = messages.saveAndFlush(newMessage(room, sender, UUID.randomUUID(), "third"))
        val baseline = Instant.parse("2026-01-01T00:00:00Z")
        setCreatedAt(first.id, baseline)
        setCreatedAt(second.id, baseline.plusSeconds(1))
        setCreatedAt(third.id, baseline.plusSeconds(2))

        val latest = messages.findLatest(room.id, PageRequest.of(0, 1))
        val before = messages.findHistoryBefore(
            room.id,
            baseline.plusSeconds(1),
            second.id,
            PageRequest.of(0, 10)
        )
        val after = messages.findHistoryAfter(
            room.id,
            baseline.plusSeconds(1),
            second.id,
            PageRequest.of(0, 10)
        )

        assertThat(latest.content.map { it.id }).containsExactly(third.id)
        assertThat(before.content.map { it.id }).containsExactly(first.id)
        assertThat(after.content.map { it.id }).containsExactly(third.id)
    }

    private fun newUser(username: String, email: String) = UserEntity(
        username = username,
        email = email
    )

    private fun newGroupRoom(owner: UserEntity): ChatRoomEntity =
        rooms.saveAndFlush(ChatRoomEntity(roomType = RoomType.GROUP, createdBy = owner))

    private fun newMessage(
        room: ChatRoomEntity,
        sender: UserEntity,
        clientMessageId: UUID,
        content: String
    ) = MessageEntity(
        clientMessageId = clientMessageId,
        room = room,
        sender = sender,
        content = content,
        messageType = MessageType.TEXT
    )

    private fun setCreatedAt(messageId: UUID, createdAt: Instant) {
        jdbcTemplate.update(
            "UPDATE messages SET created_at = ? WHERE id = ?",
            Timestamp.from(createdAt),
            messageId
        )
    }
}
