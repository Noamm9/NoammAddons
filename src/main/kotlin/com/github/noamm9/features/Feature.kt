package com.github.noamm9.features

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.Savable
import com.github.noamm9.event.Event
import com.github.noamm9.event.EventBus.EventContext
import com.github.noamm9.event.EventListener
import com.github.noamm9.event.EventPriority
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.features.annotations.Dev
import com.github.noamm9.ui.clickgui.CategoryType
import com.github.noamm9.ui.clickgui.componnents.Setting
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.Utils.spaceCaps
import net.minecraft.client.gui.GuiGraphics

open class Feature(
    val description: String? = null,
    name: String? = null,
    toggled: Boolean = false,
) {
    val name = name ?: this::class.simpleName.toString().spaceCaps()
    val listeners = mutableSetOf<EventListener<*>>()

    val configSettings = mutableSetOf<Setting<*>>()
    val hudElements = mutableSetOf<HudElement>()

    @JvmField
    var enabled = toggled

    private val isDev = this::class.java.isAnnotationPresent(Dev::class.java)
    val alwaysActive = this::class.java.isAnnotationPresent(AlwaysActive::class.java)

    open val category = if (isDev) CategoryType.DEV else initCategory()

    protected inline val mc get() = NoammAddons.mc
    protected inline val scope get() = NoammAddons.scope
    protected inline val cacheData get() = NoammAddons.cacheData

    fun initialize() {
        if (enabled || alwaysActive) onEnable() else onDisable()

        init()
    }

    open fun init() {}


    open fun onEnable() {
        listeners.forEach(EventListener<*>::register)
    }

    open fun onDisable() {
        listeners.forEach(EventListener<*>::unregister)
    }

    open fun toggle() {
        enabled = ! enabled
        if (enabled) onEnable()
        else onDisable()

        if (mc.isOfflineDeveloperMode) {
            ChatUtils.modMessage("$name: ${if (enabled) "&aEnabled" else "&cDisabled"}")
        }
    }

    protected inline fun <reified T: Event> register(
        priority: EventPriority = EventPriority.NORMAL,
        noinline block: EventContext<T>.() -> Unit
    ): EventListener<T> {
        val listener = EventListener(T::class.java, priority, block)
        listeners.add(listener)
        return listener
    }

    fun hudElement(
        name: String,
        enabled: () -> Boolean = { true },
        shouldDraw: () -> Boolean = { true },
        render: (GuiGraphics, Boolean) -> Pair<Float, Float>
    ): HudElement {
        return object: HudElement() {
            override val name = name
            override val toggle: Boolean get() = this@Feature.enabled && shouldDraw.invoke()
            override val shouldDraw: Boolean get() = enabled.invoke()
            override fun draw(ctx: GuiGraphics, example: Boolean): Pair<Float, Float> = render(ctx, example)
        }.also(hudElements::add)
    }

    fun getSettingByName(key: String?): Setting<*>? {
        return configSettings.find { it.name == key && it is Savable }
    }

    private fun initCategory(): CategoryType {
        val parts = this::class.java.`package` !!.name.split(".")
        val categoryName = parts[parts.indexOf("impl") + 1].uppercase()
        if (CategoryType.entries.none { it.name.equals(categoryName, true) }) throw Error("Category does not exist: $categoryName")
        return CategoryType.valueOf(categoryName.uppercase())
    }
}