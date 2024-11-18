package noammaddons.features.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ClockDisplay: Feature() {
    private val ClockDisplay = TextElement(
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
        config.ClockDisplayColor, hudData.getData().ClockDisplay
    )

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (! config.ClockDisplay) return

        GlStateManager.pushMatrix()

        ClockDisplay.run {
            setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            setColor(config.ClockDisplayColor)
            draw(false)
        }

        GlStateManager.popMatrix()
    }
}
