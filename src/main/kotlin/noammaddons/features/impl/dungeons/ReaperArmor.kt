package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionUtils.reaperSwap
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.Utils.equalsOneOf

// todo cooldown timer?
object ReaperArmor: Feature("auto reaper armor swap, /na ras") {
    val autoReaperArmorSlot = SliderSetting("Auto Reaper Armor Slot", 1, 9, 1, 1)
    private val autoSwap = ToggleSetting("M7 Dragons Auto Swap")
    override fun init() = addSettings(autoReaperArmorSlot, autoSwap)

    private var timer: Int? = null

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! autoSwap.value) return
        if (event.component.noFormatText.matches(Regex("\\[BOSS] Wither King: You... again\\?"))) return
        if (thePlayer?.clazz?.equalsOneOf(Archer, Berserk) != true) return
        timer = 134
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        timer?.let { timer = timer !! - 1 }
        if (timer == 0) {
            timer = null
            reaperSwap()
        }
    }
}
