package com.github.noamm9.features

import com.github.noamm9.config.Savable
import com.github.noamm9.event.Event
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventContext
import com.github.noamm9.event.EventListener
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.SettingProvider
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.spaceCaps
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW

open class Feature(
    val description: String? = null,
    name: String? = null,
    toggled: Boolean = false,
    toggleKeybind: Boolean = false,
): Shortcuts, SettingProvider {
    private val alwaysActive = this::class.java.isAnnotationPresent(AlwaysActive::class.java)
    val name = name ?: this::class.simpleName.toString().spaceCaps()
    open val category = initCategory()
    @JvmField var enabled = toggled
    open val toggleKeybindSetting: KeybindSetting? = if (toggleKeybind) KeybindSetting("Toggle") else null

    override val configSettings = mutableSetOf<Setting<*>>()
    val listeners = mutableSetOf<EventListener<*>>()
    val hudElements = mutableSetOf<HudElement>()

    fun initialize() {
        toggleKeybindSetting?.let {
            configSettings.add(it)
            registerToggleKeybind(it)
        }
        init()

        if (enabled || alwaysActive) onEnable() else onDisable()
    }

    open fun init() = Unit
    open fun onEnable() = listeners.forEach(EventListener<*>::register)
    open fun onDisable() {
        if (alwaysActive) return
        listeners.forEach(EventListener<*>::unregister)
    }

    open fun toggle() {
        enabled = ! enabled
        if (enabled || alwaysActive) onEnable()
        else onDisable()
    }

    protected inline fun <reified T: Event> register(
        priority: EventPriority = EventPriority.NORMAL,
        receiveCancelled: Boolean = false,
        noinline block: EventContext<T>.() -> Unit
    ): EventListener<T> {
        val listener = EventBus.listener<T>(priority, receiveCancelled, block)
        listeners.add(listener)
        return listener
    }

    fun hudElement(
        name: String,
        enabled: () -> Boolean = { true },
        shouldDraw: () -> Boolean = { true },
        centered: Boolean = false,
        render: (GuiGraphicsExtractor, Boolean) -> Pair<Float, Float>,
    ): HudElement {
        return object: HudElement() {
            override val name = name
            override val toggle: Boolean get() = this@Feature.enabled && enabled.invoke()
            override val shouldDraw: Boolean get() = shouldDraw.invoke()
            override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Float, Float> = render(ctx, example)
            override val centered = centered
        }.also(hudElements::add)
    }

    fun getSettingByName(key: String?) = configSettings.find { it.name == key && it is Savable }

    private fun initCategory(): CategoryType {
        val parts = this::class.java.`package` !!.name.split(".")
        val categoryName = parts[parts.indexOf("impl") + 1].uppercase()
        if (CategoryType.entries.none { it.name.equals(categoryName, true) }) error("Category does not exist: $categoryName")
        return CategoryType.valueOf(categoryName.uppercase())
    }

    private fun registerToggleKeybind(bind: KeybindSetting) {
        EventBus.register<KeyboardEvent.KeyPressed> {
            if (mc.screen != null || event.action != GLFW.GLFW_PRESS) return@register
            if (! bind.matches(event.keyEvent.key, false)) return@register

            this@Feature.toggle()
        }

        EventBus.register<MouseClickEvent> {
            if (mc.screen != null || event.action != GLFW.GLFW_PRESS) return@register
            if (! bind.matches(event.button, true)) return@register

            this@Feature.toggle()
        }
    }
}