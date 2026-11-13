package com.example.chatapp_backend.media.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream

@Component
@ConditionalOnProperty(prefix = "app.media", name = ["backend"], havingValue = "s3")
class S3MediaStorage(
    private val s3: S3Client,
    private val properties: S3MediaProperties
) : MediaStorage {
    override fun put(storageKey: String, bytes: ByteArray, contentType: String) {
        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .key(storageKey)
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .serverSideEncryption("AES256")
            .build()
        s3.putObject(request, RequestBody.fromBytes(bytes))
    }

    override fun open(storageKey: String): InputStream = s3.getObject(
        GetObjectRequest.builder().bucket(properties.bucket).key(storageKey).build()
    )

    override fun delete(storageKey: String) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket).key(storageKey).build())
    }
}
