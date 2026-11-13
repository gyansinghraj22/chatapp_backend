package com.example.chatapp_backend.media.controller

import com.example.chatapp_backend.auth.security.AuthenticatedUser
import com.example.chatapp_backend.media.dto.MediaUploadResponse
import com.example.chatapp_backend.media.service.MediaService
import jakarta.validation.constraints.NotNull
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.util.UUID

@RestController
@RequestMapping("/api/media")
class MediaController(private val mediaService: MediaService) {
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestPart("file") @NotNull file: MultipartFile
    ): ResponseEntity<MediaUploadResponse> = ResponseEntity.ok(mediaService.upload(principal.id, file))

    @GetMapping("/{mediaId}")
    fun download(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @PathVariable mediaId: UUID
    ): ResponseEntity<InputStreamResource> {
        val media = mediaService.download(principal.id, mediaId)
        val disposition = ContentDisposition.attachment()
            .filename(media.fileName, StandardCharsets.UTF_8)
            .build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(media.contentType))
            .contentLength(media.size)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff")
            .body(InputStreamResource(media.inputStream))
    }
}
