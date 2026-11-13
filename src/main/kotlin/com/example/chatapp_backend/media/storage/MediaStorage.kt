package com.example.chatapp_backend.media.storage

import java.io.InputStream

/** Binary storage only; PostgreSQL stores metadata and an opaque key, never file bytes. */
interface MediaStorage {
    fun put(storageKey: String, bytes: ByteArray, contentType: String)
    fun open(storageKey: String): InputStream
    fun delete(storageKey: String)
}
