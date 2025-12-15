package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.editgui.GuiElement
import noammaddons.config.editgui.HudEditorScreen
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ServerUtils
import noammaddons.utils.Utils.favoriteColor

object TpsDisplay: Feature() {
    private val color by ColorSetting("Color", favoriteColor, false)

    private object TpsDisplayElement: GuiElement(hudData.getData().tpsDisplay) {
        override val enabled: Boolean get() = TpsDisplay.enabled
        val text get() = "TPS: &f${ServerUtils.averageTps}"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() {
            if (HudEditorScreen.isOpen()) return
            drawText(text, getX(), getY(), getScale(), color)
        }

        override fun exampleDraw() = drawText("TPS: &f20", getX(), getY(), getScale(), color)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! TpsDisplayElement.enabled) return
        TpsDisplayElement.draw()
    }
}