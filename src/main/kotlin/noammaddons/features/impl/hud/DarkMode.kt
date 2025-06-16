/**
 * DarkMode Feature
 * Changes the opacity of your game overlay for a dark mode effect.
 * Part of Noamm Addons.
 *
 * @author axle.coffee
 * @since 2025-06-15
 */
package noammaddons.features.impl.hud

import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.utils.RenderUtils
import java.awt.Color

/**
 * Object for toggling dark mode overlay.
 */
object DarkMode : Feature(
    "Changes the opacity of your game -- > you like playing for 14 hours dont you :boykisser:"
) {

    /**
     * Opacity slider setting (0-100).
     */
    private val levelOfYourBlackness by SliderSetting(
        "Opacity", 0, 100, 5, 10,
    )

    /**
     * Renders the dark overlay after the HUD.
     *
     * @param event RenderGameOverlayEvent.Text
     */
    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Text) {
        // TODO: Overlay this AFTER the HUD is rendered, so it doesn't cover the HUD (e.g. noamm gui texts etc like clock etc)
        val opacity = levelOfYourBlackness / 100f
        val width = mc.displayWidth
        val height = mc.displayHeight

        RenderUtils.drawRect(
            Color(0, 0, 0, (opacity * 255).toInt()),
            0, 0, width, height,
        )
    }
}