package com.github.noamm9.config

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.google.common.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.*
import kotlin.reflect.KProperty

class PogObject<T: Any>(val fileName: String, @Volatile private var data: T, private val type: Type) {
    private val dataFile = File("config/$MOD_NAME/$fileName.json").apply {
        parentFile.mkdirs()
    }

    init {
        data = loadData()
        objects.add(this)
        save()
    }

    private fun loadData(): T {
        if (! dataFile.exists()) return data
        return try {
            val content = dataFile.readText().takeUnless(String::isNullOrBlank) ?: return data
            if (content == "null") return data
            GsonUtils.gson.fromJson<T>(content, type) ?: error("data is null?")
        }
        catch (e: Exception) {
            NoammAddons.logger.warn("PogObject: failed to load $fileName", e)
            e.printStackTrace()
            throw e
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = data
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        data = value
        save()
    }

    fun get(): T = data
    fun set(newData: T) = ::data.set(newData)

    @Synchronized
    fun save() = try {
        dataFile.writeText(GsonUtils.gson.toJson(data, type))
    }
    catch (e: Exception) {
        NoammAddons.logger.warn("PogObject: failed to save $fileName", e)
        e.printStackTrace()
        throw e
    }

    companion object {
        private val objects = ArrayList<PogObject<*>>()

        inline operator fun <reified T: Any> invoke(fileName: String, defaultObject: T) =
            PogObject(fileName, defaultObject, object: TypeToken<T>() {}.type)

        init {
            ThreadUtils.loop(TimeUnit.MINUTES.toMillis(5)) { objects.forEach(PogObject<*>::save) }
            ThreadUtils.addShutdownHook { objects.forEach(PogObject<*>::save) }
        }
    }
}