package com.github.noamm9.config

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.google.common.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.*
import java.util.concurrent.locks.*
import kotlin.concurrent.withLock

class PogObject<T: Any>(val fileName: String, val defaultData: T, private val type: Type) {
    private val dataFile = File("config/$MOD_NAME/$fileName.json")
    @Volatile private var data = defaultData
    private val saveLock = ReentrantLock()

    init {
        dataFile.parentFile.mkdirs()
        data = loadData()
        objects.add(this)
    }

    private fun loadData(): T {
        if (! dataFile.exists()) return defaultData
        return try {
            val raw = dataFile.readText().takeUnless(String::isBlank) ?: return defaultData
            GsonUtils.gson.fromJson<T>(raw, type) ?: defaultData
        }
        catch (e: Exception) {
            try {
                val corruptFile = File(dataFile.parentFile, "${dataFile.name}.corrupt")
                Files.move(dataFile.toPath(), corruptFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                NoammAddons.logger.warn("PogObject: failed to load $fileName. the file is probably corrupted check ${corruptFile.name}", e)
            }
            catch (backupEx: Exception) {
                NoammAddons.logger.error("PogObject: failed to save corrupted file $fileName", backupEx)
            }

            defaultData
        }
    }

    fun get() = data

    fun set(newData: T) {
        data = newData
        save()
    }

    fun update(block: T.() -> Unit) {
        synchronized(this) {
            data.apply(block)
        }
        save()
    }

    fun save() = saveLock.withLock {
        try {
            val tempFile = File(dataFile.parentFile, "${dataFile.name}.tmp")
            tempFile.writeText(GsonUtils.gson.toJson(data, type))
            Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
        catch (e: Exception) {
            NoammAddons.logger.warn("PogObject: failed to save $fileName.json", e)
        }
    }

    companion object {
        private val objects = CopyOnWriteArrayList<PogObject<*>>()

        inline operator fun <reified T: Any> invoke(fileName: String, defaultObject: T) =
            PogObject(fileName, defaultObject, object: TypeToken<T>() {}.type)

        init {
            ThreadUtils.loop(TimeUnit.MINUTES.toMillis(5)) { objects.forEach(PogObject<*>::save) }
            ThreadUtils.addShutdownHook { objects.forEach(PogObject<*>::save) }
        }
    }
}