package noammaddons.features.impl.hud

import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import java.awt.Color

object FpsDisplay: Feature() {
    private val color by ColorSetting("Color", Color(255, 0, 255), false)

    private object FpsDisplayElement: GuiElement(hudData.getData().FPSdisplay) {
        override val enabled get() = FpsDisplay.enabled
        private val text get() = "${Minecraft.getDebugFPS()} fps"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale(), color)
    }

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! FpsDisplayElement.enabled) return
        FpsDisplayElement.draw()
    }
}
