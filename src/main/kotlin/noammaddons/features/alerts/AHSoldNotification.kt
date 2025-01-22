package noammaddons.features.alerts

import net.minecraft.event.ClickEvent.Action.RUN_COMMAND
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.Alert
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.NumbersUtils.format


object AHSoldNotification: Feature() {
    private val regex = Regex("^\\[Auction] (.+) bought (.+) for (.+) coins CLICK$")
    // https://regex101.com/r/9uHJqC/1

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.SoldAHNotification) return
        val chatComponent = event.component

        val matchResult = regex.find(chatComponent.noFormatText) ?: return
        val (buyer, item, price) = matchResult.destructured
        val components = listOf(chatComponent) + chatComponent.siblings
        var command = ""

        for (component in components) {
            if (component !is ChatComponentText) continue
            val clickEvent = component.chatStyle.chatClickEvent ?: continue
            if (clickEvent.action != RUN_COMMAND) continue

            command = clickEvent.value
        }

        if (command.isBlank()) return
        Alert(
            "§bSold AH Notification",
            "§6$buyer §7bought §6$item §7for §6${format(price)} §7coins",
            5, { sendChatMessage(command) }
        )
    }
}

