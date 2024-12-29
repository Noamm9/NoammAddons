package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.NumbersUtils.toFixed


object PhoenixPet: Feature() {
    private val regex = Regex("^Your Phoenix Pet saved you from certain death!$")
    private val PhoenixPetElement = TextElement("&cPhoenix Pet: &aREADY", dataObj = hudData.getData().PhoenixPet)
    private var timer = 0L
    private var draw = false
    private const val petCooldown = 60_000

    val phoenixCD get() = ((petCooldown + (timer - System.currentTimeMillis())).toDouble() / 1000).toFixed(1).toDouble()

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! event.component.unformattedText.removeFormatting().matches(regex)) return
        timer = System.currentTimeMillis()

        if (config.PhoenixPetDisplay) draw = true
        if (config.PhoenixPetAlert) showTitle("&5Phoenix Pet")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.PhoenixPetDisplay) return
        if (! draw) return

        PhoenixPetElement.run {
            setText(
                when {
                    phoenixCD > 0.0 -> "&cPhoenix Pet: &a$phoenixCD"
                    (phoenixCD == 0.0 || phoenixCD > - 30.0) -> "&cPhoenix Pet: &aREADY"
                    else -> return
                }
            )
            draw()
        }
    }
}
