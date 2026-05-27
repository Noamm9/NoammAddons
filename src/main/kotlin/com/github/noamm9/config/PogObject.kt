package com.github.noamm9.config

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.google.common.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.*
import kotlin.reflect.KProperty

class PogObject<T: Any>(fileName: String, val defaultObject: T, private val type: Type) {
    private val dataFile = File("config/${NoammAddons.MOD_NAME}/$fileName.json").also {
        it.parentFile.mkdirs()
        objects.add(this)
    }

    @Volatile private var data: T = load()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = data
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        data = value
        save()
    }

    fun get(): T = data
    fun save() = dataFile.writeText(GsonUtils.gson.toJson(data, type))

    private fun load(): T {
        if (! dataFile.exists() || dataFile.length() == 0L) return defaultObject
        return GsonUtils.gson.fromJson(dataFile.readText(), type)
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