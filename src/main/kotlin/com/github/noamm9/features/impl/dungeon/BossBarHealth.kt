package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ILerpingBossEvent
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.location.LocationUtils
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.network.chat.Component
import java.util.*
import kotlin.math.roundToInt

object BossBarHealth: Feature(name = "Bossbar Health", description = "Shows the health number of the bossbar boss.") {
    private val theWatcher by ToggleSetting("The Watcher", true)
    private val f4Thorn by ToggleSetting("Thorn", true)
    private val f7Withers by ToggleSetting("F7 Withers", true)

    @JvmStatic
    fun onRender(instance: LerpingBossEvent, original: Operation<Component>): Component? {
        val originalName = original.call(instance)
        if (! enabled) return originalName
        if (! LocationUtils.inDungeon) return originalName
        val maxHealth = getMaxHealth(originalName) ?: return originalName

        val percent = (instance as ILerpingBossEvent).getTargetPrecent()
        val currentHealth = (percent * maxHealth).roundToInt().toFloat()

        return originalName.copy().append(
            Component.literal(" §r§8- §a" + formatHealth(currentHealth) + "§7/§a" + formatHealth(maxHealth) + "§c❤")
        )
    }

    @JvmStatic
    fun getMaxHealth(nameComponent: Component): Float? {
        val name = nameComponent.unformattedText
        val isMaster = LocationUtils.isMasterMode

        return when (name) {
            "The Watcher" if theWatcher.value -> 12F + (LocationUtils.dungeonFloorNumber?.toFloat() ?: 0F)
            "Thorn" if f4Thorn.value -> if (isMaster) 6F else 4F
            "Maxor" if f7Withers.value -> if (isMaster) 800_000_000F else 100_000_000F
            "Storm" if f7Withers.value -> if (isMaster) 1_000_000_000F else 400_000_000F
            "Goldor" if f7Withers.value -> if (isMaster) 1_200_000_000F else 750_000_000F
            "Necron" if f7Withers.value -> if (isMaster) 1_400_000_000F else 1_000_000_000F
            else -> null
        }
    }

    @JvmStatic
    fun formatHealth(health: Float): String {
        if (health >= 1_000_000_000) {
            val h = health / 1_000_000_000F
            if (h % 1.0f == 0f) return h.toInt().toString() + "B"
            return String.format(Locale.US, "%.1fB", h)
        }
        else if (health >= 1_000_000) return (health / 1_000_000F).toInt().toString() + "M"
        else if (health >= 1000) return (health / 1000f).toInt().toString() + "k"
        else return health.toInt().toString()
    }
}