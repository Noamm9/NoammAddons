package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.highlight
import java.awt.Color
import kotlin.jvm.optionals.getOrNull

object SalvageOverlay: Feature("Highlights salvageable dungeon gear.") {
    private val under50 by ColorSetting("Highlight Color", Color.cyan.withAlpha(160))
    private val base50 by ColorSetting("50% stats Color", Color.RED.withAlpha(160))

    override fun init() {
        register<ContainerEvent.Render.Slot.Pre> {
            if (! LocationUtils.inSkyblock) return@register
            val stack = event.slot.item.takeUnless { it.isEmpty } ?: return@register
            if (stack in PlayerUtils.getArmor()) return@register
            if (stack.hoverName.string.contains("✪")) return@register
            val statBoost = stack.customData.getInt("baseStatBoostPercentage").getOrNull() ?: return@register
            event.slot.highlight(event.context, if (statBoost == 50) base50.value else under50.value)
        }
    }
}