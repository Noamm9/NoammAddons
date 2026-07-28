package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.NoammAddons
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class Setting<T>(val name: String, val defaultValue: T) {
    open var value: T = defaultValue
        set(value) {
            if (NoammAddons.isLoaded) {
                changeListener?.invoke(value)
            }
            field = value
        }

    var x = 0
    var y = 0

    var width = 0
    open val height: Int get() = 20

    var headerName: String? = null
    var description: String? = null

    var visibility: () -> Boolean = { true }
    var changeListener: ((T) -> Unit)? = null

    fun reset() = ::value.set(defaultValue)

    abstract fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int)
    abstract fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean

    open fun mouseReleased(button: Int) {}
    open fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double) = false

    open fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) = false
    open fun charTyped(codePoint: Char) = false
}