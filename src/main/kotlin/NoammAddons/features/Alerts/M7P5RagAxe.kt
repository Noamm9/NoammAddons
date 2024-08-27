package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.LocationUtils.F7Phase
import NoammAddons.utils.RenderUtils.drawText
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object M7P5RagAxe {
    private var showtitle = false
    private val Time = 3000
    private val Text = "&6USE RAGNAROCK AXE!".addColor()

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 3 || !config.M7P5RagAxe || F7Phase != 5 || event.message.unformattedText.removeFormatting() != "[BOSS] Wither King: You... again?") return

        Thread {
            showtitle = true
            Thread.sleep(Time.toLong())
            showtitle = false
        }.start()
    }


    @SubscribeEvent
    fun title(event: RenderGameOverlayEvent.Post) {
        if (!showtitle || mc.ingameGUI == null || event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        drawText(
            Text,
            (ScaledResolution(mc).scaledWidth) / 2 - (mc.fontRendererObj.getStringWidth(Text.removeFormatting()) * 4.5) / 2,
            ScaledResolution(mc).scaledHeight / 2 - 10 * 4.5,
            3.6
        )
    }
}