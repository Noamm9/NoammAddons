package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.config.EditGui.HudElement
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.toFixed
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SpiritMask {
    private val SpiritMaskElement = HudElement("&fSpirit Mask: &aREADY", dataObj = hudData.getData().SpiritMask)
    private val maskCooldown = 30_000
    private var timer = 0L
    private var draw = false

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!config.SpiritMaskDisplay) return
        if (event.type.toInt() != 0) return

        if (!event.message.unformattedText.removeFormatting().matches(Regex("^Second Wind Activated! Your Spirit Mask saved your life!$"))) return
        timer = System.currentTimeMillis()
        draw = true
    }

    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent.Pre) {
        if (!config.SpiritMaskDisplay) return
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return
        if (!draw) return

        val cooldown = ((maskCooldown + (timer - System.currentTimeMillis())).toDouble() / 1000).toFixed(1).toDouble()

        SpiritMaskElement.setText(
            when {
                cooldown > 0.0 -> "&fSpirit Mask: &a$cooldown"
                (cooldown == 0.0 || cooldown > -30.0) -> "&fSpirit Mask: &aREADY"
                else -> return
            }
        ).draw()
    }
}
