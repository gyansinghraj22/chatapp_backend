package com.example.chatapp_backend.chat.controller

import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.chat.dto.MessageHistoryResponse
import com.example.chatapp_backend.chat.dto.MessageResponse
import com.example.chatapp_backend.chat.dto.SendMessageRequest
import com.example.chatapp_backend.chat.service.MessageService
import jakarta.validation.Valid
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.validation.annotation.Validated
import java.security.Principal
import java.time.Instant
import java.util.UUID

@Controller
@Validated
class MessageController(
    private val messageService: MessageService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    @PostMapping("/api/rooms/{roomId}/messages")
    @ResponseBody
    fun sendRest(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable roomId: UUID,
        @Valid @RequestBody request: SendMessageRequest
    ): MessageResponse = publish(messageService.send(principal.id, roomId, request))

    @GetMapping("/api/rooms/{roomId}/messages")
    @ResponseBody
    fun history(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable roomId: UUID,
        @RequestParam(required = false) beforeCreatedAt: Instant?,
        @RequestParam(required = false) beforeMessageId: UUID?,
        @RequestParam(defaultValue = "50") size: Int
    ): MessageHistoryResponse = messageService.history(principal.id, roomId, beforeCreatedAt, beforeMessageId, size)

    @MessageMapping("/chat.send")
    fun sendStomp(payload: StompSendMessageRequest, principal: Principal) {
        val authenticated = principal as? AuthenticatedUser
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated websocket principal required")
        val result = messageService.send(authenticated.id, payload.roomId, payload.message)
        publish(result)
    }

    private fun publish(response: MessageResponse): MessageResponse {
        // The proxied transactional service has committed by the time this method returns.
        messagingTemplate.convertAndSend("/topic/rooms/${response.roomId}/messages", response)
        return response
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleStompError(ex: Exception): Map<String, String> = mapOf(
        "error" to (ex.message ?: "Unable to process message")
    )
}

data class StompSendMessageRequest(
    val roomId: UUID,
    @field:Valid val message: SendMessageRequest
)
