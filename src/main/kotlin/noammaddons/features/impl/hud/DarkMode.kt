package noammaddons.features.impl.hud

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import java.awt.Color

object DarkMode: Feature("Applies a dark tint to your game") {
    private val strength by SliderSetting("Opacity", 0f, 100f, 1f, 20f)

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        val color = Color.BLACK.withAlpha(strength / 100f).rgb
        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, 0f, 400f)
        Gui.drawRect(0, 0, mc.getWidth(), mc.getHeight(), color)
        GlStateManager.popMatrix()
    }
}