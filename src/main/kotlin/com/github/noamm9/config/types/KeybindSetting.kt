package com.github.noamm9.config.types

import com.github.noamm9.compat.*
import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils.gsonObject
import com.google.gson.JsonElement
import com.mojang.blaze3d.platform.InputConstants

class KeybindSetting(
    name: String,
    defaultValue: Int = InputConstants.UNKNOWN.value
): ConfigHolder<Int>(name, defaultValue), Savable {
    var scanCode = 0
    var isMouse = false

    private var previousState = false

    fun displayName(): String {
        if (value == InputConstants.UNKNOWN.value) return "NONE"
        val type = if (isMouse) InputConstants.Type.MOUSE else KEYBOARD_TYPE
        return type.getOrCreate(value).displayName.string.uppercase()
    }

    fun isDown(): Boolean {
        if (value == InputConstants.UNKNOWN.value) return false
        return if (isMouse) isMouseDown(value)
        else isKeyDown(value)
    }

    fun isPressed(): Boolean {
        val currentState = isDown()
        val wasPressed = ! previousState && currentState
        previousState = currentState
        return wasPressed
    }

    fun matches(code: Int, mouse: Boolean) = value != InputConstants.UNKNOWN.value && isMouse == mouse && value == code

    override fun write() = gsonObject {
        writeInputSystem()
        addProperty("key", value)
        addProperty("scan", scanCode)
        addProperty("isMouse", isMouse)
    }

    override fun read(element: JsonElement) = element.asJsonObject.let { obj ->
        value = obj.get("key").asInt
        scanCode = obj.get("scan").asInt
        isMouse = obj.get("isMouse").asBoolean
        if (isLegacyKeybind(obj)) {
            value = migrateLegacyKey(value, isMouse)
            scanCode = 0
        }
    }
}