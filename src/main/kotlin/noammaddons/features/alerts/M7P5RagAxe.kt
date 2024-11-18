package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.SoundUtils.iHaveNothing
import noammaddons.utils.ThreadUtils.setTimeout

object M7P5RagAxe: Feature() {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.M7P5RagAxe) return
        if (F7Phase != 5) return
        if (event.component.unformattedText.removeFormatting() != "[BOSS] Wither King: You... again?") return

        setTimeout(1600) { iHaveNothing.start() }
        setTimeout(2000) { showTitle("rag", rainbow = true) }
    }
}