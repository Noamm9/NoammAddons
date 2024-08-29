package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.showTitle
import NoammAddons.utils.LocationUtils.F7Phase
import NoammAddons.utils.RenderUtils.drawText
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object M7P5RagAxe {
    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 3 || !config.M7P5RagAxe || F7Phase != 5) return
        if (event.message.unformattedText.removeFormatting() != "[BOSS] Wither King: You... again?") return
        showTitle("&6USE RAGNAROCK AXE!".addColor())
    }
}