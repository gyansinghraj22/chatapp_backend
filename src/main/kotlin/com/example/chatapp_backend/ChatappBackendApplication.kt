package com.example.chatapp_backend

import com.example.chatapp_backend.auth.security.JwtProperties
import com.example.chatapp_backend.persistence.entity.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.autoconfigure.domain.EntityScan

@SpringBootApplication
@EntityScan(basePackageClasses = [UserEntity::class])
@EnableJpaRepositories(basePackages = ["com.example.chatapp_backend.persistence.repository"])
@EnableConfigurationProperties(JwtProperties::class)
class ChatappBackendApplication

fun main(args: Array<String>) {
    runApplication<ChatappBackendApplication>(*args)
}
