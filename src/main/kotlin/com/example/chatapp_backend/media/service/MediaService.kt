package com.example.chatapp_backend.media.service

import com.example.chatapp_backend.media.dto.MediaUploadResponse
import com.example.chatapp_backend.media.storage.MediaStorage
import com.example.chatapp_backend.persistence.entity.MediaUploadEntity
import com.example.chatapp_backend.persistence.entity.MediaUploadStatus
import com.example.chatapp_backend.persistence.repository.MediaUploadRepository
import com.example.chatapp_backend.persistence.repository.UserRepository
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class MediaService(
    private val storage: MediaStorage,
    private val mediaUploads: MediaUploadRepository,
    private val users: UserRepository,
    @Value("\${app.media.max-file-bytes:15728640}") private val maxFileBytes: Long
) {
    private val tika = Tika()
    private val allowedTypes = setOf(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "text/plain", "text/csv"
    )

    @Transactional
    fun upload(ownerId: UUID, file: MultipartFile): MediaUploadResponse {
        val owner = users.findById(ownerId).filter { it.isActive }
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active account required") }
        if (maxFileBytes <= 0) throw IllegalStateException("Media max-file-bytes must be positive")
        if (file.isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty")
        if (file.size > maxFileBytes) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the upload size limit")
        val bytes = try { file.bytes } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file", ex)
        }
        if (bytes.size.toLong() > maxFileBytes) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the upload size limit")

        val detectedType = tika.detect(bytes)
        if (detectedType !in allowedTypes) throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File type is not permitted")
        val declared = file.contentType?.substringBefore(';')?.trim()?.lowercase()
        if (!declared.isNullOrBlank() && declared != "application/octet-stream" && declared != detectedType) {
            throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Declared content type does not match file contents")
        }

        val safeName = safeFileName(file.originalFilename)
        val key = "${owner.id}/${UUID.randomUUID()}"
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        storage.put(key, bytes, detectedType)
        try {
            val entity = mediaUploads.saveAndFlush(
                MediaUploadEntity(
                    owner = owner,
                    storageKey = key,
                    originalFileName = safeName,
                    contentType = detectedType,
                    fileSize = bytes.size.toLong(),
                    sha256 = sha256,
                    status = MediaUploadStatus.UPLOADED,
                    expiresAt = Instant.now().plusSeconds(86_400)
                )
            )
            return response(entity)
        } catch (ex: Exception) {
            runCatching { storage.delete(key) }
            throw ex
        }
    }

    @Transactional(readOnly = true)
    fun download(ownerId: UUID, mediaId: UUID): MediaDownload {
        val record = mediaUploads.findByIdAndOwner_Id(mediaId, ownerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")
        if (record.status == MediaUploadStatus.DELETED) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")
        val input = try { storage.open(record.storageKey) } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media object is unavailable", ex)
        }
        return MediaDownload(record.originalFileName, record.contentType, record.fileSize, input)
    }

    private fun response(entity: MediaUploadEntity) = MediaUploadResponse(
        id = entity.id,
        fileName = entity.originalFileName,
        contentType = entity.contentType,
        size = entity.fileSize,
        sha256 = entity.sha256,
        status = entity.status.name,
        downloadUrl = "/api/media/${entity.id}",
        expiresAt = entity.expiresAt
    )

    private fun safeFileName(input: String?): String {
        val basename = input.orEmpty().replace('\\', '/').substringAfterLast('/')
        val cleaned = basename.filter { it.code >= 32 && it != '\u007f' }.trim().take(255)
        return cleaned.ifBlank { "upload" }
    }
}

data class MediaDownload(val fileName: String, val contentType: String, val size: Long, val inputStream: InputStream)
