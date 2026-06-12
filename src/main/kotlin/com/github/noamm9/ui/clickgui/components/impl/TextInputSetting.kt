package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.config.Savable
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.render.Render2D
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import org.lwjgl.glfw.GLFW
import java.awt.Color

class TextInputSetting(name: String, defaultValue: String): Setting<String>(name, defaultValue), Savable {
    private val handler = TextInputHandler(
        textProvider = { value },
        textSetter = { value = it; notifyChange() }
    )

    private val hoverAnim = Animation(200L)
    override val height get() = 38

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        hoverAnim.update(if (isHovered || handler.listening) 1f else 0f)

        Style.drawBackground(ctx, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        Style.drawHoverBar(ctx, x.toFloat(), y.toFloat(), height.toFloat(), hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 4f, hoverAnim.value)

        val bx = x + 8f
        val by = y + 15f
        val bw = width - 16f
        val bh = 20f

        Render2D.drawRect(ctx, bx, by, bw, bh, Color(10, 10, 10, 180))
        Render2D.drawRect(ctx, bx, by + bh - 1f, bw * hoverAnim.value, 1f, Style.accentColor)

        handler.x = bx
        handler.y = by
        handler.width = bw
        handler.height = bh

        handler.draw(ctx, mouseX.toFloat(), mouseY.toFloat())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val event = MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, GLFW.GLFW_PRESS))
        return handler.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), event)
    }

    override fun mouseReleased(button: Int) {
        handler.mouseReleased()
    }

    override fun charTyped(codePoint: Char): Boolean {
        val event = CharacterEvent(codePoint.code)
        return handler.keyTyped(event)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val event = KeyEvent(keyCode, scanCode, modifiers)
        return handler.keyPressed(event)
    }

    override fun write() = JsonPrimitive(value)
    override fun read(element: JsonElement?) {
        value = element?.jsonPrimitive?.contentOrNull ?: return
    }
}