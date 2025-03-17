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


object BonzoMask: Feature() {
    private val regex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")

    private object BonzoMaskElement: GuiElement(hudData.getData().BonzoMask) {
        var text = "&9Bonzo Mask: &aREADY"
        override val enabled: Boolean get() = config.BonzoMaskDisplay
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private const val maskCooldown = 180 * 20
    private var timer = - 1
    private var draw = false

    val bonzoCD get() = timer / 20f

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! event.component.noFormatText.matches(regex)) return
        timer = maskCooldown

        if (config.BonzoMaskDisplay) draw = true
        if (config.BonzoMaskAlert) showTitle("&9Bonzo Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.BonzoMaskDisplay) return
        if (! draw) return

        BonzoMaskElement.text = when {
            bonzoCD > 0 -> "&9Bonzo Mask: &a${bonzoCD.toFixed(1)}"
            (bonzoCD == 0f || bonzoCD > - 30.0) -> "&9Bonzo Mask: &aREADY"
            else -> return
        }

        BonzoMaskElement.draw()
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        timer -= 1
    }
}
