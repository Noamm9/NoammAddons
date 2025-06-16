package noammaddons.features.impl.hud

import gg.essential.elementa.utils.withAlpha
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils
import java.awt.Color


object DarkMode: Feature("Applys a dark tint to your game") {
    private val levelOfYourBlackness by SliderSetting("Opacity", 0f, 100f, 1f, 20f)

    // we want this on highest so stuff like maps/timers are still normal color/opacity/blackness rah
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderOverlay(event: RenderOverlay) {
        val color = Color.BLACK.withAlpha(levelOfYourBlackness / 100f)
        val width = mc.getWidth()
        val height = mc.getHeight()
        RenderUtils.drawRect(color, 0, 0, width, height)
    }
}