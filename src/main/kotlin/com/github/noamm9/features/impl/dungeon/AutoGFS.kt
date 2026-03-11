package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils

object AutoGFS: Feature("Automatically refills dungeon items from your sacks using /gfs while in dungeons.") {
    private val delay by SliderSetting("Delay (seconds)", 20.0, 1.0, 60.0, 1.0)
        .withDescription("How often to check for refills.")

    private val refillPearl by ToggleSetting("Refill Pearl")
    private val refillTNT by ToggleSetting("Refill TNT")
    private val refillJerry by ToggleSetting("Refill Jerry")
    private val refillLeaps by ToggleSetting("Refill Leaps")


    override fun init() {
        ThreadUtils.loop({ delay.value * 1000 }) {
            refill()
        }
    }

    private fun refill() {
        if (! enabled || ! LocationUtils.inDungeon) return
        if (mc.screen != null || mc.player == null) return
        if (DungeonListener.thePlayer?.isDead == true) return
        val inventory = mc.player?.inventory ?: return

        var pearlCount = 0
        var jerryCount = 0
        var tntCount = 0
        var leapCount = 0

        for (stack in inventory) {
            if (stack.isEmpty) continue

            when (stack.skyblockId) {
                "ENDER_PEARL" -> pearlCount += stack.count
                "INFLATABLE_JERRY" -> jerryCount += stack.count
                "SUPERBOOM_TNT" -> tntCount += stack.count
                "SPIRIT_LEAP" -> leapCount += stack.count
            }
        }

        checkAndRefill(pearlCount, 16, "ender_pearl", refillPearl.value)
        checkAndRefill(jerryCount, 64, "inflatable_jerry", refillJerry.value)
        checkAndRefill(tntCount, 64, "superboom_tnt", refillTNT.value)
        checkAndRefill(leapCount, 16, "spirit_leap", refillLeaps.value)
    }

    private fun checkAndRefill(current: Int, max: Int, gfsName: String, toggle: Boolean) {
        if (! toggle) return
        if (current == 0) return
        val needed = max - current
        if (needed >= 4) {
            ChatUtils.sendCommand("gfs $gfsName $needed")
        }
    }
}
//#endif