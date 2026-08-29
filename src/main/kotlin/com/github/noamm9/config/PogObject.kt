package com.github.noamm9.config

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.utils.FileHandler
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.catch
import com.google.common.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.*

class PogObject<T>(fileName: String, val defaultData: T, private val type: Type) {
    private val fileHandler = FileHandler(File("config/$MOD_NAME/$fileName.json"))
    @Volatile private var data = run {
        objects.add(this)
        val raw = fileHandler.read().takeUnless(String::isBlank) ?: return@run defaultData
        catch { GsonUtils.gson.fromJson(raw, type) } ?: defaultData
    }

    fun get() = data
    fun set(newData: T) = ::data.set(newData).also { save() }
    fun save() = fileHandler.write(GsonUtils.gson.toJson(data, type))

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