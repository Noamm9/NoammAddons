package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.favoriteColor
import noammaddons.utils.Utils.remove

object TpsDisplay: Feature() {
    private val color by ColorSetting("Color", favoriteColor, false)

    private object TpsDisplayElement: GuiElement(hudData.getData().tpsDisplay) {
        override val enabled: Boolean get() = TpsDisplay.enabled
        var text = "TPS: &f20"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() {
            if (HudEditorScreen.isOpen()) return
            drawText(text, getX(), getY(), getScale(), color)
        }

        override fun exampleDraw() = drawText("TPS: &f20", getX(), getY(), getScale(), color)
    }

    fun getTps() = "TPS: ${TpsDisplayElement.text.removeFormatting().remove("TPS: ").toInt().coerceAtMost(20)}"
    private var tps = 0

    private var tickTimer = 0

    @SubscribeEvent
    fun onTick(event: Tick) {
        tickTimer ++

        if (tickTimer != 20) return
        TpsDisplayElement.text = "TPS: &f$tps"
        tickTimer = 0
        tps = 0
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        tps ++
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        tps = 0
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! TpsDisplayElement.enabled) return
        TpsDisplayElement.draw()
    }
}