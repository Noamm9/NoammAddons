package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ActionUtils.reaperSwap
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils.Classes.Archer
import noammaddons.utils.DungeonUtils.Classes.Berserk
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf

object AutoReaperArmorSwap: Feature() {

    @SubscribeEvent
    fun autoReaperArmorSwap(event: Chat) {
        if (! config.AutoReaperArmorSwap) return
        if (event.component.noFormatText != "[BOSS] Wither King: You... again?") return
        if (thePlayer?.clazz?.equalsOneOf(Archer, Berserk) != true) return

        setTimeout(6700) { reaperSwap() }
    }
}
