package NoammAddons.features.alerts

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.utils.ChatUtils.showTitle
import NoammAddons.sounds.chipi_chapa
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting

object RNGSound {
    private val regex = Regex("^RNG METER! Reselected the (.+?) for .+ CLICK HERE to select a new drop!$")
    // RNG METER! Reselected the Tarantula Talisman for Spider Slayer! CLICK HERE to select a new drop!

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val msg = event.message.unformattedText.removeFormatting()

        if (msg.matches(regex) && config.RNGSound && event.type.toInt() in 0..1) {
            val rngDrop = regex.find(msg)?.groupValues?.get(1)
            showTitle(rngDrop ?: return)
            mc.thePlayer.playSound("$MOD_ID:chipi_chapa", 1f, 1f)
            chipi_chapa.play()
        }
    }
}

/*
    Note:
    0 and 1 = chat message
    2 = action bar message
*/