package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.SoundUtils.chipiChapa

object RNGSound: Feature() {
    private val regex = Regex("^&d&lRNG METER! &r&aReselected the (.+?) &afor .+ &e&lCLICK HERE &r&ato select a new drop!&r$".addColor())
    // RNG METER! Reselected the Tarantula Talisman for Spider Slayer! CLICK HERE to select a new drop!

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.RNGSound) return
        val rngDrop = regex.find(event.component.formattedText)?.destructured?.component1() ?: return

        chipiChapa.start()
        showTitle(rngDrop, time = 7)
    }
}