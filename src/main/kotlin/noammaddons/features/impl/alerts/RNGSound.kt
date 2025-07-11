package noammaddons.features.impl.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.SoundUtils

object RNGSound: Feature("Plays a sound when you get an rng drop") {
    private val regex = Regex("^&d&lRNG METER! &r&aReselected the (.+?) &afor .+ &e&lCLICK HERE &r&ato select a new drop!&r$".addColor())
    // RNG METER! Reselected the Tarantula Talisman for Spider Slayer! CLICK HERE to select a new drop!

    private var lastPlayed = System.currentTimeMillis()

    @SubscribeEvent
    fun onChat(event: Chat) {
        val rng = regex.find(event.component.formattedText)?.destructured?.component1() ?: return
        if (System.currentTimeMillis() - lastPlayed < 20_000) return@onChat
        lastPlayed = System.currentTimeMillis()

        SoundUtils.chipiChapa()
        showTitle(rng, time = 7)
    }
}