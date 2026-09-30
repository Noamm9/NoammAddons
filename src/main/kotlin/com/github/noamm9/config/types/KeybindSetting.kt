package com.github.noamm9.config.types

//? if 26.2 {
/*import com.github.noamm9.NoammAddons.mc*/
//? }
import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
//? if >=26.3 {
import com.github.noamm9.config.migrators.LegacyKeybinds
//? }
import com.github.noamm9.utils.GsonUtils.gsonObject
import com.google.gson.JsonElement
import com.mojang.blaze3d.platform.InputConstants
//? if <26.2 {
/*import gg.essential.universal.UKeyboard
*///? } else if <26.3 {
/*import org.lwjgl.glfw.GLFW*/
//? } else {
import org.lwjgl.sdl.SDLMouse
//? }

class KeybindSetting(
    name: String,
    //? if <26.2 {
    /*defaultValue: Int = UKeyboard.KEY_NONE
    *///? } else {
    defaultValue: Int = InputConstants.UNKNOWN.value
    //? }
): ConfigHolder<Int>(name, defaultValue), Savable {
    var scanCode = 0
    var isMouse = false

    private var previousState = false

    fun displayName(): String {
        //? if <26.2 {
        /*if (value == UKeyboard.KEY_NONE) return "NONE"
        val type = if (isMouse) InputConstants.Type.MOUSE else InputConstants.Type.KEYSYM
        *///? } else if <26.3 {
        /*if (value == InputConstants.UNKNOWN.value) return "NONE"
        val type = if (isMouse) InputConstants.Type.MOUSE else InputConstants.Type.KEYSYM*/
        //? } else {
        if (value == InputConstants.UNKNOWN.value) return "NONE"
        val type = if (isMouse) InputConstants.Type.MOUSE else InputConstants.Type.KEYBOARD
        //? }
        return type.getOrCreate(value).displayName.string.uppercase()
    }

    fun isDown(): Boolean {
        //? if <26.2 {
        /*if (value == UKeyboard.KEY_NONE) return false
        return UKeyboard.isKeyDown(value)
        *///? } else if <26.3 {
        /*if (value == InputConstants.UNKNOWN.value) return false
        return if (isMouse) GLFW.glfwGetMouseButton(mc.window.handle(), value) == GLFW.GLFW_PRESS
        else InputConstants.isKeyDown(mc.window, value)*/
        //? } else {
        if (value == InputConstants.UNKNOWN.value) return false
        return if (isMouse) SDLMouse.SDL_GetMouseState(null, null) and (1 shl (value - 1)) != 0
        else InputConstants.isKeyDown(value)
        //? }
    }

    fun isPressed(): Boolean {
        val currentState = isDown()
        val wasPressed = ! previousState && currentState
        previousState = currentState
        return wasPressed
    }

    //? if <26.2 {
    /*fun matches(code: Int, mouse: Boolean) = value != UKeyboard.KEY_NONE && isMouse == mouse && value == code
    *///? } else {
    fun matches(code: Int, mouse: Boolean) = value != InputConstants.UNKNOWN.value && isMouse == mouse && value == code
    //? }

    override fun write() = gsonObject {
        //? if >=26.3 {
        addProperty("inputSystem", "SDL")
        //? }
        addProperty("key", value)
        addProperty("scan", scanCode)
        addProperty("isMouse", isMouse)
    }

    override fun read(element: JsonElement) = element.asJsonObject.let { obj ->
        value = obj.get("key").asInt
        scanCode = obj.get("scan").asInt
        isMouse = obj.get("isMouse").asBoolean
        //? if >=26.3 {
        if (obj.get("inputSystem")?.asString != "SDL") {
            value = LegacyKeybinds.from(value, isMouse)
            scanCode = 0
        }
        //? }
    }
}