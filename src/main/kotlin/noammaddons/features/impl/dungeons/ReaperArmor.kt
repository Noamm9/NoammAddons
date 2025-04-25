package noammaddons.features.impl.dungeons

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionUtils.reaperSwap
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.Utils.equalsOneOf

// todo cooldown timer?
object ReaperArmor: Feature("auto reaper armor swap, /na ras") {
    val autoReaperArmorSlot = SliderSetting("Auto Reaper Armor Slot", 1, 9, 1.0)
    private val autoSwap = ToggleSetting("M7 Dragons Auto Swap")
    private var timer: Int? = null

    override fun init() {
        onServerTick {
            timer?.let { timer = timer !! - 1 }
            if (timer == 0) {
                timer = null
                reaperSwap()
            }
        }

        onChat(Regex("\\[BOSS] Wither King: You... again\\?")) {
            if (! autoSwap.value) return@onChat
            if (thePlayer?.clazz?.equalsOneOf(Archer, Berserk) != true) return@onChat
            timer = 134
        }

        addSettings(autoSwap)
    }
}
