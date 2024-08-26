package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.RenderUtils.drawText
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object ThunderBottle {
    private var type = 0
    private val noThunderBottle = "&e&l⚠ &4No Thunder Bottle &e&l⚠ ".addColor()
    private val fullThunderBottle = "&e&l⚠ &9&lTHUNDER BOTTLE FULL &e&l⚠ ".addColor()
    private val regex = Regex("-+\n.+entered MM The Catacombs, Floor VII!\n-+")

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (regex.find(event.message.unformattedText) != null && config.NoThunderBottleAlert) {
            if (!(mc.thePlayer.inventory.mainInventory.any { it?.displayName?.removeFormatting() == "Empty Thunder Bottle" })) {
                type = 1
                return
            }
        }

        if (event.message.unformattedText.equals("> Your bottle of thunder has fully charged!") && config.FullThunderBottleAlert) {
            type = 2
            return
        }
    }


    @SubscribeEvent
    fun title(event: RenderGameOverlayEvent.Post) {
        if (type == 0 || mc.ingameGUI == null || event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        val text = if (type == 1) noThunderBottle else fullThunderBottle

        drawText(
            text,
            (ScaledResolution(mc).scaledWidth) / 2 - (mc.fontRendererObj.getStringWidth(text.removeFormatting()) * 4.5) / 2,
            ScaledResolution(mc).scaledHeight / 2 - 20 * 4.5,
            4.5
        )
    }
}