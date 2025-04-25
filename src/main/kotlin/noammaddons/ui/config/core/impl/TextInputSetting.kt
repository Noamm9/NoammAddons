package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ChatAllowedCharacters
import noammaddons.features.Feature
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.reflect.KProperty


class TextInputSetting(label: String, override val defaultValue: String): Component<String>(label), Savable {
    override var value = defaultValue

    private var focused = false
    private var caretVisible = true
    private var lastBlink = System.currentTimeMillis()
    private var cursorIndex = 0
    private val caretBlinkRate = 500L // ms
    private val padding = 6.0
    private val inputHeight = 14.0
    private val fieldWidth = width - (padding * 2)

    override val height = 25.0 + padding + inputHeight

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        drawSmoothRect(compBackgroundColor, x, y, width, height)
        drawText(name, x + padding, y + 1 + padding)

        val fieldX = x + padding
        val fieldY = y + 22.5

        val borderColor = if (focused) accentColor else hoverColor
        drawSmoothRect(borderColor, fieldX - 1, fieldY - 1, fieldWidth + 2, inputHeight + 2)
        drawSmoothRect(Color(20, 20, 20), fieldX, fieldY, fieldWidth, inputHeight)

        drawText(value, fieldX + padding, fieldY + 2)

        if (focused && caretVisible) {
            val textBeforeCaret = value.take(cursorIndex)
            val caretX = fieldX + padding + getStringWidth(textBeforeCaret)
            drawRect(Color.WHITE, caretX, fieldY + 2, 1.0, inputHeight - 4)
        }

        if (System.currentTimeMillis() - lastBlink > caretBlinkRate) {
            caretVisible = ! caretVisible
            lastBlink = System.currentTimeMillis()
        }
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        focused = mouseX in x .. (x + width) && mouseY in y .. (y + height)
        if (focused) SoundUtils.click()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (! focused) return false

        val ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
        when {
            ctrl && keyCode == Keyboard.KEY_C -> {
                GuiScreen.setClipboardString(value)
            }

            ctrl && keyCode == Keyboard.KEY_V -> {
                val paste = GuiScreen.getClipboardString()?.filter { ChatAllowedCharacters.isAllowedCharacter(it) } ?: ""
                value = value.substring(0, cursorIndex) + paste + value.substring(cursorIndex)
                cursorIndex += paste.length
            }

            ctrl && keyCode == Keyboard.KEY_BACK -> {
                val prevWord = prevWordIndex(value, cursorIndex)
                value = value.removeRange(prevWord, cursorIndex)
                cursorIndex = prevWord
            }

            keyCode == Keyboard.KEY_BACK -> {
                if (cursorIndex > 0) {
                    value = value.removeRange(cursorIndex - 1, cursorIndex)
                    cursorIndex --
                }
            }

            keyCode == Keyboard.KEY_LEFT -> cursorIndex = (cursorIndex - 1).coerceAtLeast(0)
            keyCode == Keyboard.KEY_RIGHT -> cursorIndex = (cursorIndex + 1).coerceAtMost(value.length)

            keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE -> focused = false

            ChatAllowedCharacters.isAllowedCharacter(typedChar) -> {
                val newText = value.substring(0, cursorIndex) + typedChar + value.substring(cursorIndex)
                if (getStringWidth(newText) <= fieldWidth - padding * 2) {
                    value = newText
                    cursorIndex ++
                }
            }
        }

        cursorIndex = cursorIndex.coerceIn(0, value.length)
        lastBlink = System.currentTimeMillis()
        caretVisible = true
        return true
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    private fun prevWordIndex(str: String, from: Int): Int {
        val before = str.substring(0, from)
        val lastSpace = before.lastIndexOf(' ')
        return if (lastSpace == - 1) 0 else lastSpace
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asString?.let {
            value = it
            cursorIndex = value.length
        }
    }
}
