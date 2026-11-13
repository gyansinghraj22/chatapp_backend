package com.example.chatapp_backend.media.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Component
@ConditionalOnProperty(prefix = "app.media", name = ["backend"], havingValue = "local", matchIfMissing = true)
class LocalMediaStorage(@Value("\${app.media.local-root:./uploads}") root: String) : MediaStorage {
    private val rootPath: Path = Path.of(root).toAbsolutePath().normalize()

    override fun put(storageKey: String, bytes: ByteArray, contentType: String) {
        val target = resolve(storageKey)
        Files.createDirectories(target.parent)
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    }

    override fun open(storageKey: String): InputStream = Files.newInputStream(resolve(storageKey))

    override fun delete(storageKey: String) {
        Files.deleteIfExists(resolve(storageKey))
    }

    private fun resolve(key: String): Path {
        require(key.isNotBlank() && !key.startsWith('/') && !key.contains('\\')) { "Invalid storage key" }
        val resolved = rootPath.resolve(key).normalize()
        require(resolved.startsWith(rootPath)) { "Storage key escapes configured media root" }
        return resolved
    }
}
