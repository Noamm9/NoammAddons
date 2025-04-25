package noammaddons.features.impl.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Actionbar
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.LocationUtils.inSkyblock

object SkyblockExp: Feature("Print to chat the SkyBlock XP message from the actionbar") {
    private val SkyBlockExpRegex = Regex(".*(§b\\+\\d+ SkyBlock XP §.\\([^()]+\\)§b \\(\\d+/\\d+\\)).*")
    private var lastMatch: String? = null

    @SubscribeEvent
    fun onActionbar(event: Actionbar) {
        if (! inSkyblock) return
        val match = SkyBlockExpRegex.find(event.component.formattedText)?.groupValues?.get(1) ?: return
        if (match != lastMatch) {
            lastMatch = match
            modMessage(match)
        }
    }
}

