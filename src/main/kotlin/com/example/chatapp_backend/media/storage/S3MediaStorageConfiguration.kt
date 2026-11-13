package com.example.chatapp_backend.media.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

@Configuration
@ConditionalOnProperty(prefix = "app.media", name = ["backend"], havingValue = "s3")
class S3MediaStorageConfiguration {
    @Bean
    fun s3MediaProperties(
        @Value("\${app.media.s3.bucket}") bucket: String,
        @Value("\${app.media.s3.region:us-east-1}") region: String,
        @Value("\${app.media.s3.endpoint:}") endpoint: String
    ): S3MediaProperties {
        require(bucket.isNotBlank()) { "MEDIA_S3_BUCKET is required when MEDIA_BACKEND=s3" }
        return S3MediaProperties(bucket, region, endpoint)
    }

    @Bean(destroyMethod = "close")
    fun s3Client(
        properties: S3MediaProperties,
        @Value("\${app.media.s3.path-style-access:true}") pathStyleAccess: Boolean
    ): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(properties.region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccess).build())
        if (properties.endpoint.isNotBlank()) builder.endpointOverride(URI.create(properties.endpoint))
        return builder.build()
    }
}

data class S3MediaProperties(val bucket: String, val region: String, val endpoint: String)
