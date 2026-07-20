package com.github.noamm9.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class FileHandler(val file: File) {
    private val mutex = Mutex()
    private val tmp = File(file.parentFile, "${file.name}.tmp")

    init {
        file.parentFile?.mkdirs()
        if (tmp.exists()) tmp.delete()
        if (! file.exists()) file.createNewFile()
    }

    fun read() = runBlocking { mutex.withLock { if (file.exists()) file.readText() else "" } }
    fun write(value: String) = runBlocking { mutex.withLock { atomicWrite(value) } }

    private fun atomicWrite(value: String) = try {
        FileOutputStream(tmp).use { fos ->
            fos.write(value.toByteArray())
            fos.flush()
            fos.fd.sync()
        }

        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        FileChannel.open(file.toPath(), StandardOpenOption.READ).use { channel -> channel.force(true) }
    }
    finally {
        tmp.delete()
    }
}