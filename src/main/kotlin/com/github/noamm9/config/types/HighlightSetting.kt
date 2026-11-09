package com.github.noamm9.config.types

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.config.SettingProvider
import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.world.RenderContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import java.awt.Color

class HighlightSetting(
    name: String = "Style",
    color: Color? = Color.WHITE,
    fillColor: Color? = Color(255, 255, 255, 67),
    glow: Boolean = true,
    customColor: Boolean = false,
    customFillColor: Boolean = false,
    private val aabbOffset: Boolean = false,
    defaultStyle: Style = Style.BOX,
): DropdownSetting(name, defaultStyle.ordinal, Style.entries.filter { glow || it != Style.GLOW }.map { it.displayName }), SettingProvider {
    enum class Style(val displayName: String, val outline: Boolean = false, val fill: Boolean = false) {
        BOX("Box", outline = true),
        FILLED_BOX("Filled box", outline = true, fill = true),
        FILLED("Filled", fill = true),
        GLOW("Glow");

        val isBox get() = outline || fill
    }

    init {
        require(defaultStyle.displayName in options) { "Unavailable default highlight style: $defaultStyle" }
    }

    override val configSettings = mutableSetOf<ConfigHolder<*>>()

    val style get() = Style.entries[value]

    val useCustomColor by ToggleSetting("Custom Color", ! customColor).visibleIf { customColor && color != null }
    val outline by ColorSetting("Outline Color", color ?: Color.WHITE).visibleIf {
        color != null && (! customColor || useCustomColor.value)
    }

    val useCustomFillColor by ToggleSetting("Custom Fill Color", ! customFillColor).visibleIf {
        customFillColor && fillColor != null && style.fill
    }
    val fill by ColorSetting("Fill Color", fillColor ?: Color(255, 255, 255, 67)).visibleIf {
        fillColor != null && style.fill && (! customFillColor || useCustomFillColor.value)
    }

    val depthCheck by ToggleSetting("Depth Check").visibleIf { true }
    val thickness by SliderSetting("Thickness", 4f, 1f, 8f, 0.5f).visibleIf { style.outline }
    val sizeOffset by SliderSetting("Size Offset", 0.0, -1.0, 1.0, 0.05).visibleIf { aabbOffset && style.isBox }
        .withDescription("Changes the horizontal size of the highlight box")

    private val hasOutlineColor = color != null
    private val hasFillColor = fillColor != null

    private fun <T: ConfigHolder<*>> T.visibleIf(condition: () -> Boolean): T = showIf { this@HighlightSetting.visibility() && condition() }

    override fun write() = JsonObject().apply {
        addProperty("style", style.name)
        for (setting in configSettings) add(setting.jsonName, (setting as Savable).write())
    }

    override fun read(element: JsonElement) {
        val obj = element.asJsonObject
        obj.get("style")?.asString?.let { savedStyle ->
            value = Style.entries.indexOfFirst { it.name == savedStyle && it.displayName in options }.takeIf { it >= 0 } ?: defaultValue
        }
        for (setting in configSettings) obj.get(setting.jsonName)?.let { (setting as Savable).read(it) }
    }

    override fun reset() {
        super.reset()
        configSettings.forEach(ConfigHolder<*>::reset)
    }

    fun outlineColor(fallback: Color = Color.WHITE, override: Color? = null): Color =
        override ?: if (hasOutlineColor && useCustomColor.value) outline.value else fallback

    fun fillColor(fallback: Color = Color(255, 255, 255, 67), override: Color? = null): Color =
        override ?: if (! hasFillColor) fallback else if (useCustomFillColor.value) fill.value else Color(fallback.red, fallback.green, fallback.blue, fill.value.alpha)

    fun draw(
        ctx: RenderContext,
        aabb: AABB,
        color: Color = Color.WHITE,
        fillColor: Color = Color(255, 255, 255, 67),
        overrideColor: Color? = null,
        overrideFillColor: Color? = null,
    ) {
        if (! style.isBox) return
        val box = if (aabbOffset) aabb.inflate(sizeOffset.value, 0.0, sizeOffset.value) else aabb
        ctx.renderBoxBounds(
            box, outlineColor(color, overrideColor), this.fillColor(fillColor, overrideFillColor),
            outline = style.outline, fill = style.fill, phase = ! depthCheck.value, lineWidth = thickness.value
        )
    }

    fun draw(
        ctx: RenderContext,
        entity: Entity,
        color: Color = Color.WHITE,
        fillColor: Color = Color(255, 255, 255, 67),
        overrideColor: Color? = null,
        overrideFillColor: Color? = null,
    ) = draw(ctx, entity.renderBoundingBox, color, fillColor, overrideColor, overrideFillColor)

    fun draw(event: CheckEntityGlowEvent, color: Color = Color.WHITE, overrideColor: Color? = null) {
        if (style != Style.GLOW) return
        if (depthCheck.value && mc.player?.hasLineOfSight(event.entity) != true) return
        event.color = outlineColor(color, overrideColor)
    }
}