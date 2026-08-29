package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils.jsonObject
import com.google.gson.JsonElement

class MultiCheckboxSetting(name: String, defaultValue: MutableMap<String, Boolean>): ConfigHolder<MutableMap<String, Boolean>>(name, defaultValue), Savable {
    override fun write() = jsonObject { value.forEach { (k, v) -> addProperty(k, v) } }
    override fun read(element: JsonElement) = element.asJsonObject.let { obj ->
        obj.entrySet().forEach { (k, v) -> if (v.isJsonPrimitive) value[k] = v.asBoolean }
    }
}