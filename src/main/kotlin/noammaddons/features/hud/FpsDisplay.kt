package noammaddons.features.hud

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.config.EditGui.HudElement
import noammaddons.events.RenderOverlay
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FpsDisplay {
    private val FpsDisplayElement = HudElement("${Minecraft.getDebugFPS()} fps", config.FpsDisplayColor, hudData.getData().FPSdisplay)

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun draw(event: RenderOverlay) {
        if (!config.FpsDisplay) return

        FpsDisplayElement
        .setText("${Minecraft.getDebugFPS()} fps")
        .setColor(config.FpsDisplayColor)
        .draw()
    }
}
