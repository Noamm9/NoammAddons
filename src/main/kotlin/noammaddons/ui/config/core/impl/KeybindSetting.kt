package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.lerpColor
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.reflect.KProperty

class KeybindSetting(name: String, override var defaultValue: Int = Keyboard.KEY_NONE): Component<Int>(name), Savable {
    override var value = defaultValue
    private var listening = false

    private var hoverAnimProgress = 0.0
    private var isHovered = false

    private var previousState = false

    fun isDown(): Boolean {
        return if (isMouseButton(value)) {
            Mouse.isButtonDown(getMouseButtonId(value))
        } else {
            Keyboard.isKeyDown(value)
        }
    }

    fun isPressed(): Boolean {
        val currentState = isDown()
        val wasPressed = !previousState && currentState
        previousState = currentState
        return wasPressed
    }

    private fun isMouseButton(keyCode: Int) = keyCode >= MOUSE_BUTTON_OFFSET

    private fun getMouseButtonId(keyCode: Int) = keyCode - MOUSE_BUTTON_OFFSET

    private fun getMouseButtonKeyCode(buttonId: Int) = buttonId + MOUSE_BUTTON_OFFSET

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentlyHovered = isMouseOver(x, y, mouseX, mouseY) && ! listening
        if (currentlyHovered != isHovered) {
            isHovered = currentlyHovered
            animateHover(isHovered)
        }

        val animatedBgColor = lerpColor(compBackgroundColor, hoverColor, hoverAnimProgress)
        drawSmoothRect(animatedBgColor, x, y, width, height)

        textRenderer.drawText(name, x + 5, y + 6)

        val displayText = when {
            listening -> "..."
            value == Keyboard.KEY_NONE -> "NONE"
            isMouseButton(value) -> getMouseButtonName(getMouseButtonId(value))
            else -> Keyboard.getKeyName(value)
        }

        val buttonWidth = textRenderer.getStringWidth(displayText).coerceAtLeast(22f) + 6
        val buttonHeight = 12.0
        val buttonX = x + width - buttonWidth - 8
        val buttonY = y + (height - buttonHeight) / 2

        drawSmoothRect(accentColor, buttonX, buttonY, buttonWidth, buttonHeight)
        textRenderer.drawCenteredText(displayText, buttonX + buttonWidth / 2, buttonY + 2)
    }

    private fun getMouseButtonName(buttonId: Int) = when (buttonId) {
        0 -> "LMB"
        1 -> "RMB"
        2 -> "MMB"
        else -> "Mouse $buttonId"
    }

    private fun isMouseOver(x: Double, y: Double, mouseX: Double, mouseY: Double): Boolean {
        return mouseX in x .. (x + width) && mouseY in y .. (y + height)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (isMouseOver(x, y, mouseX, mouseY)) {
            if (listening) {
                value = getMouseButtonKeyCode(button)
                listening = false
            } else if (button == 0) {
                listening = true
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (! listening) return false

        val shouldCancel = keyCode == Keyboard.KEY_ESCAPE
        this.value = if (shouldCancel) Keyboard.KEY_NONE else keyCode
        listening = false

        return true
    }

    private fun animateHover(hovering: Boolean) = scope.launch {
        val startProgress = hoverAnimProgress
        val endProgress = if (hovering) 1.0 else 0.0
        val animationDuration = 150L

        val startTime = System.currentTimeMillis()
        var elapsedTime: Long

        while (System.currentTimeMillis() - startTime < animationDuration) {
            elapsedTime = System.currentTimeMillis() - startTime
            val t = elapsedTime.toDouble() / animationDuration
            hoverAnimProgress = lerp(startProgress, endProgress, easeOutQuad(t))
            delay(7)
        }
        hoverAnimProgress = endProgress
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value
    override fun write(): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement?) {
        element?.asInt?.let { value = it }
    }

    companion object {
        private const val MOUSE_BUTTON_OFFSET = 1000
    }
}