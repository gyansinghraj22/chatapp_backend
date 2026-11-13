package com.example.chatapp_backend.notification.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
@ConditionalOnProperty(prefix = "app.notifications.firebase", name = ["enabled"], havingValue = "true")
class FirebaseConfiguration(
    @Value("\${app.notifications.firebase.credentials-file:}") private val credentialsFile: String,
    @Value("\${app.notifications.firebase.project-id:}") private val projectId: String
) {
    @Bean
    fun firebaseApp(): FirebaseApp {
        val credentials = if (credentialsFile.isNotBlank()) {
            FileInputStream(credentialsFile).use { GoogleCredentials.fromStream(it) }
        } else {
            GoogleCredentials.getApplicationDefault()
        }
        val options = FirebaseOptions.builder().setCredentials(credentials)
            .apply { if (projectId.isNotBlank()) setProjectId(projectId) }
            .build()
        return FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(options)
    }
}
