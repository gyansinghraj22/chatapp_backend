package com.example.chatapp_backend.notification.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.WebpushConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

interface PushSender {
    fun send(token: String, title: String, body: String?, data: Map<String, String>)
}

@Component
@ConditionalOnProperty(prefix = "app.notifications.firebase", name = ["enabled"], havingValue = "true")
class FirebasePushSender(private val firebaseApp: FirebaseApp) : PushSender {
    override fun send(token: String, title: String, body: String?, data: Map<String, String>) {
        val message = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body ?: "").build())
            .putAllData(data)
            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
            .setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setSound("default").build()).build())
            .setWebpushConfig(WebpushConfig.builder().putAllData(data).build())
            .build()
        FirebaseMessaging.getInstance(firebaseApp).send(message)
    }
}

@Component
@ConditionalOnProperty(prefix = "app.notifications.firebase", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class DisabledPushSender : PushSender {
    override fun send(token: String, title: String, body: String?, data: Map<String, String>) = Unit
}
