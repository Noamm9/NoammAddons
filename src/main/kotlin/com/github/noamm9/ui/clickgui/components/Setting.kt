package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.config.ConfigHolder
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class Setting<T>(val config: ConfigHolder<T>)/*: Renderable, GuiEventListener*/ {
    val name get() = config.name
    val defaultValue get() = config.defaultValue

    open var value: T
        get() = config.value
        set(value) {
            config.value = value
        }

    var x = 0
    var y = 0

    var width = 0
    open val height: Int get() = 20

    var visibility: () -> Boolean
        get() = config.visibility
        set(value) {
            config.visibility = value
        }

    fun reset() = config.reset()

    abstract fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int)
    abstract fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean

    open fun mouseReleased(button: Int) {}
    open fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double) = false

    open fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) = false
    open fun charTyped(codePoint: Char) = false
}