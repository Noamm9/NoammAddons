package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.ActionUtils.reaperSwap
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.Utils.equalsOneOf

object AutoReaperArmorSwap: Feature() {
    private var timer: Int? = null

    @SubscribeEvent
    fun autoReaperArmorSwap(event: Chat) {
        if (! config.AutoReaperArmorSwap) return
        if (event.component.noFormatText != "[BOSS] Wither King: You... again?") return
        if (thePlayer?.clazz?.equalsOneOf(Archer, Berserk) != true) return

        timer = 6700 / 20
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
