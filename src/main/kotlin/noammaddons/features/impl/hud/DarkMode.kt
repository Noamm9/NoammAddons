package noammaddons.features.impl.hud

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.gui.Gui
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlayNoCaching
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import java.awt.Color

object DarkMode: Feature("Applies a dark tint to your game") {
    private val strength by SliderSetting("Opacity", 0f, 100f, 1f, 20f)

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderOverlay(event: RenderOverlayNoCaching) {
        if (! DarkMode.enabled) return
        val color = Color.BLACK.withAlpha(strength / 100f).rgb
        Gui.drawRect(0, 0, mc.getWidth(), mc.getHeight(), color)
    }
}
