package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.Savable
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.json.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW
import java.awt.Color


class KeybindSetting(name: String, value: Int = InputConstants.UNKNOWN.value): Setting<Int>(name, value), Savable {
    var listening = false
    private val hoverAnim = Animation(200)

    var scanCode = 0
    var isMouse = false

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x, y, width, height)
        Style.drawHoverBar(ctx, x, y, height, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val bindText = when {
            listening -> "§b..."
            value == InputConstants.UNKNOWN.value -> "§7NONE"
            else -> {
                val key = if (isMouse) InputConstants.Type.MOUSE.getOrCreate(value)
                else InputConstants.Type.KEYSYM.getOrCreate(value)
                "§7" + key.displayName.string.uppercase()
            }
        }
        Render2D.drawString(ctx, bindText, x + width - bindText.width() - 8f, y + 6f, Color.WHITE)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val isInside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height

        if (listening) {
            this.value = button
            this.isMouse = true
            this.scanCode = 0
            this.listening = false
            Style.playClickSound(1f)
            return true
        }

        if (isInside) {
            listening = true
            Style.playClickSound(1f)
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false
                return true
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                value = InputConstants.UNKNOWN.value
                isMouse = false
            }
            else {
                this.value = keyCode
                this.scanCode = scanCode
                this.isMouse = false
            }
            listening = false
            return true
        }
        return false
    }

    override fun write() = buildJsonObject {
        put("key", value)
        put("scan", scanCode)
        put("isMouse", isMouse)
    }

    override fun read(element: JsonElement?) {
        val obj = element?.jsonObject ?: return
        value = obj["key"]?.jsonPrimitive?.intOrNull ?: return
        scanCode = obj["scan"]?.jsonPrimitive?.intOrNull ?: return
        isMouse = obj["isMouse"]?.jsonPrimitive?.booleanOrNull ?: false
    }

    fun isDown(): Boolean {
        if (value == InputConstants.UNKNOWN.value) return false
        return if (isMouse) GLFW.glfwGetMouseButton(mc.window.handle(), value) == GLFW.GLFW_PRESS
        else InputConstants.isKeyDown(mc.window, value)
    }

    private var previousState = false
    fun isPressed(): Boolean {
        val currentState = isDown()
        val wasPressed = ! previousState && currentState
        previousState = currentState
        return wasPressed
    }
}