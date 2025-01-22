package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.NumbersUtils.toFixed


object BonzoMask: Feature() {
    private val regex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")
    private val BonzoMaskElement = TextElement("&9Bonzo Mask: &aREADY", dataObj = hudData.getData().BonzoMask)
    private var timer = 0L
    private var draw = false
    private const val maskCooldown = 180_000

    val bonzoCD get() = ((maskCooldown + (timer - System.currentTimeMillis())).toDouble() / 1000).toFixed(1).toDouble()

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! event.component.noFormatText.matches(regex)) return
        timer = System.currentTimeMillis()

        if (config.BonzoMaskDisplay) draw = true
        if (config.BonzoMaskAlert) showTitle("&9Bonzo Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.BonzoMaskDisplay) return
        if (! draw) return

        BonzoMaskElement.run {
            setText(
                when {
                    bonzoCD > 0.0 -> "&9Bonzo Mask: &a$bonzoCD"
                    (bonzoCD == 0.0 || bonzoCD > - 30.0) -> "&9Bonzo Mask: &aREADY"
                    else -> return
                }
            )
            draw(false)
        }
    }
}
