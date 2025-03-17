package noammaddons.features.hud

import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText

object FpsDisplay: Feature() {
    private object FpsDisplayElement: GuiElement(hudData.getData().FPSdisplay) {
        override val enabled get() = config.FpsDisplay
        private val text get() = "${Minecraft.getDebugFPS()} fps"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale(), config.FpsDisplayColor)
    }

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! FpsDisplayElement.enabled) return
        FpsDisplayElement.draw()
    }
}
