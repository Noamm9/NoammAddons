package noammaddons.features.impl.dungeons

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.ThreadUtils.loop


object AutoGFS: Feature("Automatically refills certain items from your sacks.") {
    private val timerIncrements by SliderSetting("Delay (in seconds)", 1, 60, 1, 20)
    private val refillPearl by ToggleSetting("Refill Pearl")
    private val refillTNT by ToggleSetting("Refill TNT")
    private val refillJerry by ToggleSetting("Refill Jerry")
    private val refillLeaps by ToggleSetting("Refill Leaps")

    override fun init() = loop({ timerIncrements.toLong() * 1000 }) { refill() }

    fun refill() {
        if (! enabled) return
        if (! inDungeon) return
        if (mc.currentScreen != null) return
        if (thePlayer?.isDead == true) return
        val inventory = mc.thePlayer?.inventory?.mainInventory?.filterNotNull() ?: return
        inventory.find { it.skyblockID == "ENDER_PEARL" }?.takeIf { refillPearl }?.run { fillItemFromSack(16 - stackSize, "ender_pearl") }
        inventory.find { it.skyblockID == "INFLATABLE_JERRY" }?.takeIf { refillJerry }?.run { fillItemFromSack(64 - stackSize, "inflatable_jerry") }
        inventory.find { it.skyblockID == "SUPERBOOM_TNT" }.takeIf { refillTNT }?.run { fillItemFromSack(64 - stackSize, "superboom_tnt") }
        inventory.find { it.skyblockID == "SPIRIT_LEAP" }?.takeIf { refillLeaps }?.run { fillItemFromSack(16 - stackSize, "spirit_leap") }
    }

    private fun fillItemFromSack(ammount: Int, name: String) {
        if (ammount < 4) return
        ChatUtils.sendChatMessage("/gfs $name $ammount")
    }
}
