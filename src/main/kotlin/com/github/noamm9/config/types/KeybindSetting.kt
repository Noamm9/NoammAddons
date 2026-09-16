package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils.gsonObject
import com.google.gson.JsonElement
import com.mojang.blaze3d.platform.InputConstants
import com.github.noamm9.config.migrators.LegacyKeybinds
import org.lwjgl.sdl.SDLMouse

class KeybindSetting(
    name: String,
    defaultValue: Int = InputConstants.UNKNOWN.value
): ConfigHolder<Int>(name, defaultValue), Savable {
    var scanCode = 0
    var isMouse = false

    private var previousState = false

    fun displayName(): String {
        if (value == InputConstants.UNKNOWN.value) return "NONE"
        val type = if (isMouse) InputConstants.Type.MOUSE else InputConstants.Type.KEYBOARD
        return type.getOrCreate(value).displayName.string.uppercase()
    }

    fun isDown(): Boolean {
        if (value == InputConstants.UNKNOWN.value) return false
        return if (isMouse) SDLMouse.SDL_GetMouseState(null, null) and (1 shl (value - 1)) != 0
        else InputConstants.isKeyDown(value)
    }

    fun isPressed(): Boolean {
        val currentState = isDown()
        val wasPressed = ! previousState && currentState
        previousState = currentState
        return wasPressed
    }

    fun matches(code: Int, mouse: Boolean) = value != InputConstants.UNKNOWN.value && isMouse == mouse && value == code

    override fun write() = gsonObject {
        addProperty("inputSystem", "sdl_scancode")
        addProperty("key", value)
        addProperty("scan", scanCode)
        addProperty("isMouse", isMouse)
    }

    override fun read(element: JsonElement) = element.asJsonObject.let { obj ->
        value = obj.get("key").asInt
        scanCode = obj.get("scan").asInt
        isMouse = obj.get("isMouse").asBoolean
        if (obj.get("inputSystem")?.asString != "sdl_scancode") {
            value = if (isMouse) LegacyKeybinds.mouse(value) else LegacyKeybinds.keyboard(value)
            scanCode = 0
        }
    }
}