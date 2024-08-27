package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.utils.ChatUtils.showTitle
import NoammAddons.Sounds.chipi_chapa

object RNGSound {
    private val regex = Regex("^&d&lRNG METER! &r&aReselected the (.+?) &afor .+ &e&lCLICK HERE &r&ato select a new drop!&r$")

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.message.formattedText.matches(regex) && config.RNGSound && event.type.toInt() != 3) {
            val rngDrop = regex.find(event.message.formattedText)?.groupValues?.get(1)
            showTitle(rngDrop)
            chipi_chapa.play()
        }
    }
}
