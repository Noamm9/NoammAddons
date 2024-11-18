package noammaddons.features.alerts

import net.minecraft.event.ClickEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.formatNumber
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage


object AHSoldNotification: Feature() {
    private val regex = Regex("^\\[Auction] (.+) bought (.+) for (.+) coins CLICK$")
    // https://regex101.com/r/9uHJqC/1

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.SoldAHNotification) return
        val chatComponent = event.component

        val matchResult = regex.find(chatComponent.unformattedText.removeFormatting()) ?: return
        val components = listOf(chatComponent) + chatComponent.siblings
        val (buyer, item, price) = matchResult.destructured
        var command = ""

        for (component in components) {
            if (component is ChatComponentText) {
                val clickEvent = component.chatStyle.chatClickEvent ?: continue

                if (clickEvent.action == ClickEvent.Action.RUN_COMMAND) {
                    command = clickEvent.value
                }
            }
        }

        if (command.isNotEmpty()) {
            Alert(
                "§cSold AH Notification",
                "§6$buyer §7bought §6$item §7for §6${formatNumber(price)} §7coins",
                5, { sendChatMessage(command) }
            )
            event.isCanceled = true
        }
    }
}

