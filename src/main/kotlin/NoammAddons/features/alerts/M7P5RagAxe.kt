package NoammAddons.features.alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.events.Chat
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.equalsOneOf
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.showTitle
import NoammAddons.utils.LocationUtils.F7Phase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object M7P5RagAxe {
    @SubscribeEvent
    fun onChat(event: Chat) {
		if (!config.M7P5RagAxe) return
        if (F7Phase != 5) return
        if (event.component.unformattedText.removeFormatting() != "[BOSS] Wither King: You... again?")  return
	    showTitle("&6USE RAGNAROCK AXE!".addColor())
    }
}