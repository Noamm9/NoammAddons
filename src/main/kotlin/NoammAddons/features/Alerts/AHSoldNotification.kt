package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.LocationUtils.inSkyblock
import net.minecraft.event.ClickEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.utils.ChatUtils.Alert
import NoammAddons.utils.ChatUtils.formatNumber
import NoammAddons.utils.ChatUtils.removeFormatting

object AHSoldNotification {
    private val regex = Regex("\\[Auction] (.+) bought (.+) for (.+) coins CLICK")
    // https://regex101.com/r/9uHJqC/1

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!config.SoldAHNotification || !inSkyblock || event.type.toInt() == 3) return

        val matchResult = regex.find(event.message.unformattedText.removeFormatting()) ?: run {
            return
        }

        val chatComponent = event.message
        val components = listOf(chatComponent) + chatComponent.siblings

        val (buyer, item, price) = matchResult.destructured
        var command = ""

        for (component in components) {
            if (component is ChatComponentText) {
                val clickEvent: ClickEvent? = component.chatStyle.chatClickEvent

                if (clickEvent != null && clickEvent.action == ClickEvent.Action.RUN_COMMAND) {
                    command = clickEvent.value
                }
            }
        }

        if (command.isNotEmpty()) {
            Alert(
                "§cSold AH Notification",
                "§6$buyer §7bought §6$item §7for §6${formatNumber(price)} §7coins",
                3,
                {mc.thePlayer.sendChatMessage(command)}
            )
        }
    }
}

