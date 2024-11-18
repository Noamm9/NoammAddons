package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.MathUtils.toFixed

object SpiritMask: Feature() {
    private val SpiritMaskElement = TextElement("&fSpirit Mask: &aREADY", dataObj = hudData.getData().SpiritMask)
    private const val maskCooldown = 30_000L
    private var timer = 0L
    private var draw = false

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.SpiritMaskDisplay) return
        if (! event.component.unformattedText.removeFormatting().matches(
                Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")
            )
        ) return

        timer = System.currentTimeMillis()
        draw = true

        if (! config.SpiritMaskAlert) return
        showTitle("&fSpirit Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.SpiritMaskDisplay) return
        if (! draw) return

        val cooldown = ((maskCooldown + (timer - System.currentTimeMillis())) / 1000.0).toFixed(1)

        SpiritMaskElement.run {
            setText(
                when {
                    cooldown.replace(",", ".").toDouble() > 0.0 -> "&fSpirit Mask: &a$cooldown"
                    cooldown.replace(",", ".").toDouble() > - 30.0 -> "&fSpirit Mask: &aREADY"
                    else -> return
                }
            )
            draw()
        }
    }
}
