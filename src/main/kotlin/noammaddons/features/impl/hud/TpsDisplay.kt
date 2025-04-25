package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.favoriteColor
import noammaddons.utils.Utils.remove

object TpsDisplay: Feature() {
    private val color by ColorSetting("Color", favoriteColor, false)

    private object TpsDisplayElement: GuiElement(hudData.getData().TpsDisplay) {
        override val enabled: Boolean get() = TpsDisplay.enabled
        var text = "TPS: 2O"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale(), color)
        override fun exampleDraw() = drawText("TPS: 2O", getX(), getY(), getScale(), color)
    }

    fun getTps() = "TPS: ${TpsDisplayElement.text.remove("TPS: ").toInt().coerceAtMost(20)}"
    private var tps = 0

    init {
        onServerTick { tps ++ }

        loop(1000) {
            if (mc.theWorld == null) return@loop
            if (mc.thePlayer == null) return@loop

            TpsDisplayElement.text = "TPS: $tps"
            tps = 0
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! TpsDisplayElement.enabled) return
        TpsDisplayElement.draw()
    }
}