package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.MathUtils.toFixed


object BonzoMask: Feature() {
    private val BonzoMaskElement = TextElement("&9Bonzo Mask: &aREADY", dataObj = hudData.getData().BonzoMask)
    private var timer = 0L
    private var draw = false
    private const val maskCooldown = 180_000

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.BonzoMaskDisplay) return

        if (! event.component.unformattedText.removeFormatting().matches(Regex("^Your (?:. )?Bonzo's Mask saved your life!$"))) return
        timer = System.currentTimeMillis()
        draw = true

        if (! config.BonzoMaskAlert) return
        showTitle("&9Bonzo Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.BonzoMaskDisplay) return
        if (! draw) return

        val cooldown = ((maskCooldown + (timer - System.currentTimeMillis())).toDouble() / 1000).toFixed(1).toDouble()

        BonzoMaskElement.run {
            setText(
                when {
                    cooldown > 0.0 -> "&9Bonzo Mask: &a$cooldown"
                    (cooldown == 0.0 || cooldown > - 30.0) -> "&9Bonzo Mask: &aREADY"
                    else -> return
                }
            )
            draw(false)
        }
    }
}
