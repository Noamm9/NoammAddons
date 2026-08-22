package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils.jsonObject
import com.google.gson.JsonElement
import com.mojang.blaze3d.platform.InputConstants
import gg.essential.universal.UKeyboard

class KeybindSetting(
    name: String,
    defaultValue: Int = UKeyboard.KEY_NONE
): ConfigHolder<Int>(name, defaultValue), Savable {
    var scanCode = 0
    var isMouse = false

    private var previousState = false

    fun displayName(): String {
        if (value == UKeyboard.KEY_NONE) return "NONE"
        val type = if (isMouse) InputConstants.Type.MOUSE else InputConstants.Type.KEYSYM
        return type.getOrCreate(value).displayName.string.uppercase()
    }

    fun isDown(): Boolean {
        if (value == UKeyboard.KEY_NONE) return false
        return UKeyboard.isKeyDown(value)
    }

    fun isPressed(): Boolean {
        val currentState = isDown()
        val wasPressed = ! previousState && currentState
        previousState = currentState
        return wasPressed
    }

    fun matches(code: Int, mouse: Boolean) = value != UKeyboard.KEY_NONE && isMouse == mouse && value == code

    override fun write() = jsonObject {
        addProperty("key", value)
        addProperty("scan", scanCode)
        addProperty("isMouse", isMouse)
    }

    override fun read(element: JsonElement) = element.asJsonObject.let { obj ->
        value = obj.get("key").asInt
        scanCode = obj.get("scan").asInt
        isMouse = obj.get("isMouse").asBoolean
    }
}