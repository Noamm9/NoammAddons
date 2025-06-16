package noammaddons.features.impl.hud

import gg.essential.elementa.utils.withAlpha
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.utils.RenderUtils
import java.awt.Color

object DarkMode: Feature("Applys a dark tint to your game") {
    private val levelOfYourBlackness by SliderSetting("Opacity", 0f, 100f, 1f, 20f)

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return
        val color = Color.BLACK.withAlpha(levelOfYourBlackness / 100f)
        val width = event.resolution.scaledWidth
        val height = event.resolution.scaledHeight
        RenderUtils.drawRect(color, 0, 0, width, height)
    }
}