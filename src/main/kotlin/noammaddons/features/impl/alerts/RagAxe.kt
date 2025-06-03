package noammaddons.features.impl.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout

object RagAxe: Feature() {
    private val m7Alert by ToggleSetting("M7 Dragon Alert")

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! m7Alert || F7Phase != 5 || thePlayer !!.isDead) return
        if (event.component.noFormatText != "[BOSS] Wither King: You... again?") return
        setTimeout(1800) {
            SoundUtils.iHaveNothing()
            showTitle("rag", rainbow = true)
        }
    }
}