package com.github.noamm9.features.impl.general

//#if CHEAT

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import java.util.*

object TermAutoClicker: Feature(name = "Term AC", description = "Automatically uses Salvation ability when holding right click.") {
    private val cps by NumberConfig("Clicks Per Second", 5.0, 5.0, 10.0, 1.0).withDescription("How many times per second the autoclicker should click.")

    private var baseCpsDrift = cps.value
    private var lastDriftTime = 0L
    private var nextLeftClick = 0L
    private val random = Random()

    override fun init() {
        register<TickEvent.Start> {
            val now = System.currentTimeMillis()
            if (now < nextLeftClick) return@register
            if (mc.screen != null) return@register
            if (! mc.options.keyUse.isDown) return@register
            if (player.isUsingItem) return@register

            if ("ac" !in NoammAddons.debugFlags) {
                if (player.mainHandItem.skyblockId != "TERMINATOR") return@register
            }

            if (! LocationUtils.inBoss) PlayerUtils.getSelectionBlock()?.let { pos ->
                if (DungeonUtils.isSecret(pos)) return@register
            }

            nextLeftClick = getNextClick(now)
            PlayerUtils.leftClick()
        }
    }

    // asymmetric + uniform distribution
    private fun getNextClick(now: Long): Long {
        if (now - lastDriftTime > 1000) {
            val targetCps = cps.value
            baseCpsDrift = (targetCps + MathUtils.gaussianRandom(- 10, 10) / 10.0)
            lastDriftTime = now
        }

        val baseDelay = (1000.0 / baseCpsDrift).toLong()
        val offset = run {
            val gaussian = random.nextGaussian()
            if (gaussian < 0) (gaussian * 10).toLong()
            else (gaussian * 25).toLong()
        }

        var finalDelay = baseDelay + offset

        val roll = random.nextDouble()
        if (roll < 0.01) finalDelay += random.nextInt(100, 250)
        else if (roll < 0.03) finalDelay = random.nextInt(5, 15).toLong()

        return now + finalDelay
    }
}
//#endif