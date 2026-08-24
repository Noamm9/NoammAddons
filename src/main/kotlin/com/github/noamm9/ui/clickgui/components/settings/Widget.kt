package com.github.noamm9.ui.clickgui.components.settings

import com.github.noamm9.config.ConfigHolder
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class Widget<T>(val config: ConfigHolder<T>) {
    val name get() = config.name

    open var value: T
        get() = config.value
        set(value) {
            config.value = value
        }

    var x = 0
    var y = 0

    var width = 0
    open val height: Int get() = 20

    abstract fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int)
    abstract fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean

    open fun mouseReleased(button: Int) {}
    open fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double) = false

    open fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) = false
    open fun charTyped(codePoint: Char) = false
}