package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.ui.config.core.save.Savable
import org.lwjgl.input.Keyboard
import kotlin.reflect.KProperty


class KeybindSetting(name: String, override var defaultValue: Int = Keyboard.KEY_NONE): Component<Int>(name), Savable {
    override var value = defaultValue

    private var listening = false

    fun isDown() = Keyboard.isKeyDown(value)

    private var previousState = false

    fun isPressed(): Boolean {
        val currentState = isDown()
        val wasPressed = ! previousState && currentState
        previousState = currentState
        return wasPressed
    }

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        drawSmoothRect(compBackgroundColor, x, y, width, height)
        textRenderer.drawText(name, x + 5, y + 6)

        val displayText = if (listening) "..." else if (value == Keyboard.KEY_NONE) "NONE" else Keyboard.getKeyName(value)
        val buttonWidth = textRenderer.getStringWidth(displayText).coerceAtLeast(22f) + 6
        val buttonHeight = 12.0
        val buttonX = x + width - buttonWidth - 8
        val buttonY = y + (height - buttonHeight) / 2

        val color = if (value == Keyboard.KEY_NONE) hoverColor else accentColor

        drawSmoothRect(color, buttonX, buttonY, buttonWidth, buttonHeight)
        textRenderer.drawCenteredText(displayText, buttonX + buttonWidth / 2, buttonY + 2)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        listening = mouseX in x .. x + width && mouseY in y .. y + height && button == 0
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (! listening) return false
        var cencel = false

        this.value = when (keyCode) {
            Keyboard.KEY_ESCAPE, Keyboard.KEY_NONE -> {
                cencel = true
                Keyboard.KEY_NONE
            }

            else -> keyCode
        }

        listening = false
        return cencel
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asInt?.let {
            value = it
        }
    }
}
