package com.github.noamm9.features.impl.general

//#if CHEAT

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType

object AutoGFS: Feature("Automatically refills items from your sacks using /gfs while in Skyblock.") {
    private val delay by SliderSetting("Check Delay", 20.0, 1.0, 60.0, 1.0, "s").withDescription("How often to check for refills.")

    private val refillPearl by ToggleSetting("Refill Pearl")
    private val refillTNT by ToggleSetting("Refill TNT")
    private val refillJerry by ToggleSetting("Refill Jerry")
    private val refillLeaps by ToggleSetting("Refill Leaps")

    private val kuudraTriggers = listOf("Off", "On Eaten", "On Stun")

    private val refillTwilight by ToggleSetting("Refill Twilight").section("Arrow Poison")
    private val pyTwilight by ToggleSetting("Refill after lightning", true).withDescription("M7 Archer only. Refills after the storm phase lightning message.").showIf { refillTwilight.value }
    private val p5Twilight by ToggleSetting("Refill after m7 relics", true).withDescription("M7 non-Archer/Berserk only. Refills after the Wither King relic message.").showIf { refillTwilight.value }
    private val twilightKuudra by DropdownSetting("Twilight Kuudra Trigger", 0, kuudraTriggers).withDescription("When to refill Twilight in Kuudra. On Eaten = a player is eaten, On Stun = a pod is destroyed.").showIf { refillTwilight.value }
    private val twilightAmount by SliderSetting("Twilight Amount", 8, 4, 32, 1).withDescription("The amount of Twilight to pull from your sacks.").showIf { refillTwilight.value }

    private val refillToxic by ToggleSetting("Refill Toxic")
    private val toxicKuudra by DropdownSetting("Toxic Kuudra Trigger", 0, kuudraTriggers).withDescription("When to refill Toxic in Kuudra. On Eaten = a player is eaten, On Stun = a pod is destroyed.").showIf { refillToxic.value }
    private val toxicAmount by SliderSetting("Toxic Amount", 8, 4, 32, 1).withDescription("The amount of Toxic to pull from your sacks.").showIf { refillToxic.value }

    private const val KUUDRA_EATEN_MESSAGE = "has been eaten by Kuudra!"
    private const val KUUDRA_POD_MESSAGE = "destroyed one of Kuudra's pods!"

    private val p5Message = Regex("^\\[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you\\.$")
    private val pyMessage1 = Regex("^\\[BOSS] Storm: (ENERGY HEED MY CALL|THUNDER LET ME BE YOUR CATALYST)!$")
    private var pyHappened = false

    private var kuudraStunRefilled = false

    override fun init() {
        register<WorldChangeEvent> {
            pyHappened = false
            kuudraStunRefilled = false
        }
        ThreadUtils.loop({ delay.value * 1000 }) { refill() }

        register<ChatMessageEvent> {
            val msg = event.unformattedText

            handleM7(msg)
            handleKuudra(msg)
        }
    }

    private fun handleM7(msg: String) {
        if (! refillTwilight.value) return
        if (! LocationUtils.inDungeon || ! LocationUtils.inBoss) return
        if (LocationUtils.dungeonFloor != "M7") return
        val clazz = DungeonListener.thePlayer?.clazz ?: return

        when {
            p5Twilight.value && ! clazz.equalsOneOf(DungeonClass.Archer, DungeonClass.Berserk) && p5Message.matches(msg) -> {
                ChatUtils.sendCommand("gfs twilight_arrow_poison ${twilightAmount.value}")
            }

            pyTwilight.value && ! pyHappened && clazz == DungeonClass.Archer && pyMessage1.matches(msg) -> {
                ChatUtils.sendCommand("gfs twilight_arrow_poison ${twilightAmount.value}")
                pyHappened = true
            }
        }
    }

    private fun handleKuudra(msg: String) {
        if (LocationUtils.world != WorldType.Kuudra) return

        when {
            msg.contains(KUUDRA_EATEN_MESSAGE) && ! msg.contains("Elle") -> {
                kuudraStunRefilled = false
                if (refillTwilight.value && twilightKuudra.value == 1) ChatUtils.sendCommand("gfs twilight_arrow_poison ${twilightAmount.value}")
                if (refillToxic.value && toxicKuudra.value == 1) ChatUtils.sendCommand("gfs toxic_arrow_poison ${toxicAmount.value}")
            }

            msg.contains(KUUDRA_POD_MESSAGE) && ! kuudraStunRefilled -> {
                var refilled = false
                if (refillTwilight.value && twilightKuudra.value == 2) {
                    ChatUtils.sendCommand("gfs twilight_arrow_poison ${twilightAmount.value}")
                    refilled = true
                }
                if (refillToxic.value && toxicKuudra.value == 2) {
                    ChatUtils.sendCommand("gfs toxic_arrow_poison ${toxicAmount.value}")
                    refilled = true
                }
                if (refilled) kuudraStunRefilled = true
            }
        }
    }

    private fun refill() {
        if (! enabled || ! LocationUtils.inSkyblock) return
        if (mc.screen != null) return
        val player = mc.player ?: return
        if (player.isDeadOrDying) return
        if (LocationUtils.inDungeon && DungeonListener.thePlayer?.isDead == true) return

        var pearlCount = 0
        var jerryCount = 0
        var tntCount = 0
        var leapCount = 0

        for (stack in player.inventory) when (stack.skyblockId) {
            "ENDER_PEARL" -> pearlCount += stack.count
            "INFLATABLE_JERRY" -> jerryCount += stack.count
            "SUPERBOOM_TNT" -> tntCount += stack.count
            "SPIRIT_LEAP" -> leapCount += stack.count
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
