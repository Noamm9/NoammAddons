package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.remove

object TpsDisplay: Feature() {
    private object TpsDisplayElement: GuiElement(hudData.getData().TpsDisplay) {
        override val enabled: Boolean get() = config.TpsDisplay
        var text = "TPS: 2O"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale(), config.TpsDisplayColor)
        override fun exampleDraw() = drawText("TPS: 2O", getX(), getY(), getScale(), config.TpsDisplayColor)
    }

    fun getTps() = "TPS: ${TpsDisplayElement.text.remove("TPS: ").toInt().coerceAtMost(20)}"
    private var tps = 0

    init {
        loop(1000) {
            if (mc.theWorld == null) return@loop
            if (mc.thePlayer == null) return@loop

            TpsDisplayElement.text = "TPS: $tps"
            tps = 0
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        tps ++
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! config.TpsDisplay) return
        TpsDisplayElement.draw()
    }
}