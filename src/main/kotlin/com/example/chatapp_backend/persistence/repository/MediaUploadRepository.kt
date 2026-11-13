package com.example.chatapp_backend.persistence.repository

import com.example.chatapp_backend.persistence.entity.MediaUploadEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MediaUploadRepository : JpaRepository<MediaUploadEntity, UUID> {
    fun findByIdAndOwner_Id(id: UUID, ownerId: UUID): MediaUploadEntity?
}
