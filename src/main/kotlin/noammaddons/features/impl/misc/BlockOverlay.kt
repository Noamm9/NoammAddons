package noammaddons.features.impl.misc

import gg.essential.elementa.utils.withAlpha
import net.minecraft.util.MovingObjectPosition.MovingObjectType.*
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.favoriteColor

object BlockOverlay: Feature() {
    private val mode by DropdownSetting("Mode", listOf("Outline", "Fill", "Filled Outline"))
    private val modeSetting get() = getSettingByName("Mode") as DropdownSetting
    private val phase by ToggleSetting("Phase")
    private val lineWidth by SliderSetting("Line Width", 1, 10, 1, 1).addDependency { modeSetting.value == 1 }
    private val fillColor by ColorSetting("Fill Color", favoriteColor.withAlpha(50)).addDependency { modeSetting.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", favoriteColor, false).addDependency { modeSetting.value == 1 }

    @SubscribeEvent
    fun drawBlockHighlight(event: DrawBlockHighlightEvent) = event.setCanceled(true)

    @SubscribeEvent
    fun drawBlockOverlay(event: RenderWorld) {
        if (mc.gameSettings.hideGUI) return
        mc.objectMouseOver?.takeIf { it.typeOfHit == BLOCK }?.run {
            drawBlockBox(
                blockPos = blockPos,
                overlayColor = fillColor,
                outlineColor = outlineColor,
                outline = mode.equalsOneOf(0, 2),
                fill = mode.equalsOneOf(1, 2),
                phase = phase,
                LineThickness = lineWidth.toFloat()
            )
        }
    }
}