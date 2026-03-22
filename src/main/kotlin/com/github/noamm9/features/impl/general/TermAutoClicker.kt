package com.github.noamm9.features.impl.general

//#if CHEAT

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.items.ItemUtils.skyblockId

object TermAutoClicker: Feature(name = "Term AC", description = "Automatically uses Salvation ability when holding right click.") {
    private val cps by SliderSetting("Clicks Per Second", 5, 1, 15, 1).withDescription("How many times per second the autoclicker should click.")
    private var nextLeftClick = 0L

    override fun init() {
        register<TickEvent.Start> {
            if (mc.screen != null || mc.player == null) return@register
            val player = mc.player?.takeUnless { it.isUsingItem } ?: return@register
            val now = System.currentTimeMillis()

            if (! mc.options.keyUse.isDown) return@register
            if (player.mainHandItem.skyblockId != "TERMINATOR") return@register
            if (now < nextLeftClick) return@register

            nextLeftClick = getNextClick(now)
            (mc.options.keyAttack as IKeyMapping).clickCount += 1
        }
    }

    private fun getNextClick(now: Long): Long {
        val delay = (1000.0 / cps.value).toLong()
        val randomOffset = (Math.random() * 60.0 - 30.0).toLong()
        return now + delay + randomOffset
    }
}
//#endif