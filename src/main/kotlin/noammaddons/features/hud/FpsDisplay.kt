package noammaddons.features.hud

import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature

object FpsDisplay: Feature() {
    private val FpsDisplayElement = TextElement("${Minecraft.getDebugFPS()} fps", config.FpsDisplayColor, hudData.getData().FPSdisplay)

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! config.FpsDisplay) return

        FpsDisplayElement.run {
            setText("${Minecraft.getDebugFPS()} fps")
            setColor(config.FpsDisplayColor)
            draw()
        }
    }
}
