package com.example.chatapp_backend.persistence.entity

enum class RoomType {
    PRIVATE,
    GROUP
}

enum class ParticipantRole {
    MEMBER,
    ADMIN,
    OWNER
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    FILE,
    AUDIO,
    SYSTEM
}

enum class MessageReportStatus {
    PENDING,
    REVIEWED,
    RESOLVED,
    REJECTED
}

enum class DevicePlatform {
    ANDROID,
    IOS,
    WEB
}

enum class NotificationDispatchStatus {
    PENDING,
    PROCESSING,
    RETRY,
    SUCCEEDED,
    DEAD
}
