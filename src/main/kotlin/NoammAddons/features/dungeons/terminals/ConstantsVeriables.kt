package NoammAddons.features.dungeons.terminals

import NoammAddons.NoammAddons.Companion.config
import java.awt.Color

object ConstantsVeriables {
    const val MelodyTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &dMelody"
    const val NumbersTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &9Numbers"
    const val RubixTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &bRubix"
    const val RedGreenTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &aRed &cGreen"
    const val StartWithTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &6Starts With"
    const val ColorsTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &2C&3o&4l&5o&6r"


    val lightmode = Color(203, 202, 205, 255)
    val darkmode = Color(33, 33, 33, 255)

    fun getColorMode(): Color = if (config.CustomTerminalMenuLightMode) lightmode else darkmode

    fun getSolutionColor(): Color = config.CustomTerminalMenuSolutionColor

    /**
     ** 0 = NORMAL
     ** 1 = HOVER
     ** 2 = AUTO
     */
    fun getClickMode(): Int = config.CustomTerminalMenuClickMode

    fun getTermScale(): Double = (config.CustomTerminalMenuScale * 4.0)

    data class Slot(val num: Int, val id: Int, val meta: Int, val size: Int, val name: String, val enchanted: Boolean)
}