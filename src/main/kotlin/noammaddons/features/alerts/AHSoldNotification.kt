package noammaddons.features.alerts

import noammaddons.noammaddons.Companion.config
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.formatNumber
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import net.minecraft.event.ClickEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AHSoldNotification {
    private val regex = Regex("^\\[Auction] (.+) bought (.+) for (.+) coins CLICK$")
    // https://regex101.com/r/9uHJqC/1

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (!config.SoldAHNotification) return
        val matchResult = regex.find(event.component.unformattedText.removeFormatting()) ?: return
        val (buyer, item, price) = matchResult.destructured
        var command = ""
	    
	    event.component.siblings.forEach {
			it.chatStyle?.chatClickEvent?.run {
				if (action != ClickEvent.Action.RUN_COMMAND) return@forEach
				command = value
			}
		}
	    
        if (command.isNotEmpty()) {
            Alert(
                "§cSold AH Notification",
                "§6$buyer §7bought §6$item §7for §6${formatNumber(price)} §7coins",
                6,
                { sendChatMessage(command) }
            )
            event.isCanceled = true
        }
    }
}

