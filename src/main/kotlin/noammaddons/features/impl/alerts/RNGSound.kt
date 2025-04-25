package noammaddons.features.impl.alerts

import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.SoundUtils

object RNGSound: Feature("Plays a sound when you get an rng drop") {
    private val regex = Regex("^&d&lRNG METER! &r&aReselected the (.+?) &afor .+ &e&lCLICK HERE &r&ato select a new drop!&r$".addColor())
    // RNG METER! Reselected the Tarantula Talisman for Spider Slayer! CLICK HERE to select a new drop!

    private var lastPlayed = System.currentTimeMillis()

    init {
        onChat(regex) {
            if (System.currentTimeMillis() - lastPlayed < 20_000) return@onChat
            lastPlayed = System.currentTimeMillis()

            SoundUtils.chipiChapa()
            showTitle(it.destructured.component1(), time = 7)
        }
    }
}