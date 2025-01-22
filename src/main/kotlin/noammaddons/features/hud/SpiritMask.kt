package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.NumbersUtils.toFixed

object SpiritMask: Feature() {
    private val regex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")
    private val SpiritMaskElement = TextElement("&fSpirit Mask: &aREADY", dataObj = hudData.getData().SpiritMask)
    private const val maskCooldown = 30_000L
    private var timer = 0L
    private var draw = false

    val spiritCD get() = ((maskCooldown + (timer - System.currentTimeMillis())).toDouble() / 1000).toFixed(1).toDouble()

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! event.component.noFormatText.matches(regex)) return
        timer = System.currentTimeMillis()

        if (config.SpiritMaskDisplay) draw = true
        if (config.SpiritMaskAlert) showTitle("&fSpirit Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.SpiritMaskDisplay) return
        if (! draw) return

        SpiritMaskElement.run {
            setText(
                when {
                    spiritCD > 0.0 -> "&fSpirit Mask: &a$spiritCD"
                    spiritCD > - 30.0 -> "&fSpirit Mask: &aREADY"
                    else -> return
                }
            )
            draw()
        }
    }
}
