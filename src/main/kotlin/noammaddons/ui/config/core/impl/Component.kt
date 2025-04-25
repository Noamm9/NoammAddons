package noammaddons.ui.config.core.impl

import noammaddons.features.Feature
import noammaddons.utils.RenderUtils.drawRoundedRect
import java.awt.Color
import kotlin.reflect.KProperty

abstract class Component<T>(val name: String) {
    private val dependencies = mutableListOf<Pair<Component<*>, (Component<*>) -> Boolean>>()

    abstract val defaultValue: T
    open var value: T = defaultValue

    open val width: Double = 200.0
    open val height: Double = 20.0
    var hidden = false


    open fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {}
    open fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {}
    open fun mouseDragged(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {}
    open fun mouseRelease(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {}
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean = false


    @Suppress("UNCHECKED_CAST")
    fun <D: Component<*>> addDependency(dependentOn: D, condition: (D) -> Boolean): Component<T> {
        dependencies.add(dependentOn to { c -> condition(c as D) })
        return this
    }

    fun addDependency(dependentOn: Component<Boolean>): Component<T> {
        addDependency(dependentOn) { ! it.value }
        return this
    }

    fun updateVisibility() {
        hidden = dependencies.any { (comp, condition) -> condition(comp) }
    }

    operator fun provideDelegate(thisRef: Feature, property: KProperty<*>): Component<T> {
        thisRef.register(this)
        return this
    }

    abstract operator fun getValue(thisRef: Feature, property: KProperty<*>): T


    companion object {
        val accentColor = Color(58, 142, 240)
        val compBackgroundColor = Color(30, 30, 30)
        val hoverColor = Color(42, 42, 42)

        fun easeOutQuad(t: Double) = 1 - (1 - t) * (1 - t)

        fun drawSmoothRect(color: Color, x: Number, y: Number, width: Number, height: Number) {
            drawRoundedRect(color, x, y, width, height, 2)
        }
    }
}
