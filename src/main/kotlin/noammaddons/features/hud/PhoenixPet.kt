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


object PhoenixPet: Feature() {
    private object PhoenixPetElement: GuiElement(hudData.getData().PhoenixPet) {
        override val enabled get() = config.PhoenixPetDisplay
        var text = "&cPhoenix Pet: &aREADY"
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private val regex = Regex("^Your Phoenix Pet saved you from certain death!$")
    private const val petCooldown = 60 * 20
    private var timer = - 1
    private var draw = false

    val phoenixCD get() = timer / 20f

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! event.component.noFormatText.matches(regex)) return
        timer = petCooldown

        if (config.PhoenixPetDisplay) draw = true
        if (config.PhoenixPetAlert) showTitle("&5Phoenix Pet")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! config.PhoenixPetDisplay) return
        if (! draw) return

        PhoenixPetElement.text = when {
            phoenixCD > 0 -> "&cPhoenix Pet: &a${phoenixCD.toFixed(1)}"
            (phoenixCD == 0f || phoenixCD > - 30.0) -> "&cPhoenix Pet: &aREADY"
            else -> return
        }

        PhoenixPetElement.draw()

    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        timer --
    }
}
