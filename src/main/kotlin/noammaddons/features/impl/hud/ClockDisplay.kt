package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.editgui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import java.awt.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ClockDisplay: Feature() {
    private val seconds by ToggleSetting("Show Seconds", true)
    private val color by ColorSetting("Color", Color(255, 134, 0), false)

    private object ClockDisplayElement: GuiElement(hudData.getData().clockDisplay) {
        override val enabled get() = ClockDisplay.enabled
        private val text get() = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm${if (seconds) ":ss" else ""}"))
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale(), color)
    }

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! ClockDisplayElement.enabled) return
        ClockDisplayElement.draw()
    }
}
