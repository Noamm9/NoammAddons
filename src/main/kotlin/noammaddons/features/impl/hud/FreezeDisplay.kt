package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.editgui.GuiElement
import noammaddons.config.editgui.HudEditorScreen
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.LocationUtils
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import java.awt.Color

object FreezeDisplay: Feature(desc = "Shows how long the server froze after a chosen threshold.") {
    private val color by ColorSetting("Color", Color(245, 73, 39), false)
    private val threshold by SliderSetting("Threshold", 50, 2000, 1, 500)
    private val dungeonsOnly by ToggleSetting("Only In Dungeons", true)

    private var lastTick = System.currentTimeMillis()

    private object FreezeDisplayElement: GuiElement(hudData.getData().freezeDisplay) {
        override val enabled: Boolean get() = FreezeDisplay.enabled
        val text get() = "567ms"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() {
            if (HudEditorScreen.isOpen()) return
            if (dungeonsOnly && !LocationUtils.inDungeon) return
            val tickDelta = System.currentTimeMillis() - lastTick
            if (tickDelta < threshold) return

            drawText("${tickDelta}ms", getX(), getY(), getScale(), color)
        }

        override fun exampleDraw() = drawText("567ms", getX(), getY(), getScale(), color)
    }

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (!FreezeDisplayElement.enabled) return
        FreezeDisplayElement.draw()
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        lastTick = System.currentTimeMillis()
    }
}
