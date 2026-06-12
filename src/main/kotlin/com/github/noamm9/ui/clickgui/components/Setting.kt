package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.SeparatorSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.reflect.KProperty

abstract class Setting<T>(val name: String, val defaultValue: T) {
    open var value: T = defaultValue
        set(value) {
            changeListener?.invoke(value)
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

    companion object {
        fun <T: Setting<*>> T.section(name: String): T {
            this.headerName = name
            return this
        }

        fun <T: Setting<*>> T.withDescription(desc: String): T {
            this.description = desc.let {
                return@let if (! it.endsWith('.')) "$it."
                else it
            }
            return this
        }

        fun <T, S: Setting<T>> S.onChange(listener: (T) -> Unit): S {
            this.changeListener = listener
            return this
        }

        fun <T: Setting<*>> T.showIf(condition: () -> Boolean): T {
            this.visibility = condition
            return this
        }

        fun <T: Setting<*>> T.hideIf(condition: () -> Boolean): T {
            this.visibility = { ! condition() }
            return this
        }


        operator fun <T, S: Setting<T>> S.provideDelegate(thisRef: Feature, prop: KProperty<*>): S {
            this.headerName?.let { name ->
                if (thisRef.configSettings.isNotEmpty()) {
                    thisRef.configSettings.add(SeparatorSetting().also { it.visibility = this.visibility })
                }
                thisRef.configSettings.add(CategorySetting(name).also {
                    it.visibility = this.visibility
                })
            }

            thisRef.configSettings.add(this)
            return this
        }

        operator fun <T, S: Setting<T>> S.getValue(thisRef: Feature, prop: KProperty<*>): S {
            return this
        }
    }
}