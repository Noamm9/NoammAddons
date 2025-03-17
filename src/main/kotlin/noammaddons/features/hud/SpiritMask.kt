package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText

object SpiritMask: Feature() {
    private object SpiritMaskElement: GuiElement(hudData.getData().SpiritMask) {
        override val enabled get() = config.SpiritMaskDisplay
        var text = "&fSpirit Mask: &aREADY"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 0f

        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private val regex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")
    private const val maskCooldown = 30 * 20
    private var timer = - 1
    private var draw = false

    val spiritCD get() = timer / 20f

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! event.component.noFormatText.matches(regex)) return
        timer = maskCooldown

        if (config.SpiritMaskDisplay) draw = true
        if (config.SpiritMaskAlert) showTitle("&fSpirit Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.SpiritMaskDisplay) return
        if (! draw) return

        SpiritMaskElement.text = when {
            spiritCD > 0 -> "&fSpirit Mask: &a${spiritCD.toFixed(1)}"
            spiritCD > - 30 -> "&fSpirit Mask: &aREADY"
            else -> return
        }

        SpiritMaskElement.draw()
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        timer --
    }
}
