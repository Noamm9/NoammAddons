package com.github.noamm9.features

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.SettingProvider
import com.github.noamm9.event.*
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.init.RemoteFeatures
import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.spaceCaps
import net.minecraft.client.gui.GuiGraphicsExtractor

open class Feature(
    val description: String? = null,
    name: String? = null,
    toggled: Boolean = false
): Shortcuts, SettingProvider {
    val name = name ?: this::class.simpleName.toString().spaceCaps()
    open val category = initCategory()
    @JvmField var enabled = toggled

    private val alwaysActive = this::class.java.isAnnotationPresent(AlwaysActive::class.java)
    private val remotelyDisabled get() = RemoteFeatures.isDisabled(this::class.java.simpleName)

    override val configSettings = mutableSetOf<ConfigHolder<*>>()
    val listeners = mutableSetOf<EventListener<*>>()
    val hudElements = mutableSetOf<HudElement>()

    fun initialize() {
        init()

        if (remotelyDisabled) {
            enabled = false
            return
        }

        if (enabled || alwaysActive) onEnable() else onDisable()
    }

    open fun init() = Unit
    open fun onEnable() = listeners.forEach(EventListener<*>::register)
    open fun onDisable() {
        if (alwaysActive) return
        listeners.forEach(EventListener<*>::unregister)
    }

    open fun toggle() {
        if (remotelyDisabled) return NotificationManager.push("Config GUI", "&b$name&f is temporarly disabled.")

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
        render: (GuiGraphicsExtractor, Boolean) -> Pair<Number, Number>,
    ): HudElement {
        return object: HudElement() {
            override val name = name
            override val toggle: Boolean get() = this@Feature.enabled && enabled.invoke()
            override val shouldDraw: Boolean get() = shouldDraw.invoke()
            override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Number, Number> = render(ctx, example)
            override val centered = centered
        }.also(hudElements::add)
    }

    private fun initCategory(): CategoryType {
        val parts = this::class.java.`package` !!.name.split(".")
        val categoryName = parts[parts.indexOf("impl") + 1].uppercase()
        if (CategoryType.entries.none { it.name.equals(categoryName, true) }) error("Category does not exist: $categoryName")
        return CategoryType.valueOf(categoryName.uppercase())
    }
}