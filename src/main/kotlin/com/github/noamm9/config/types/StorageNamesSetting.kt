package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils.gsonObject
import com.google.gson.JsonElement
import java.awt.Color

class StorageNamesSetting(name: String): ConfigHolder<Map<Int, StorageName>>(name, emptyMap()), Savable {
    override fun write() = gsonObject {
        value.forEach { (index, entry) ->
            add(index.toString(), gsonObject {
                addProperty("name", entry.name)
                entry.color?.let { addProperty("color", it.rgb) }
            })
        }
    }

    override fun read(element: JsonElement) {
        value = element.asJsonObject.entrySet().mapNotNull { (key, entry) ->
            val index = key.toIntOrNull()?.takeIf { it in 0 until 27 } ?: return@mapNotNull null
            val data = entry.asJsonObject
            index to StorageName(data.get("name")?.asString.orEmpty(), data.get("color")?.let { Color(it.asInt) })
        }.toMap()
    }
}

data class StorageName(val name: String = "", val color: Color? = null)
