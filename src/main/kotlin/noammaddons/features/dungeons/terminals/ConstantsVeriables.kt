package noammaddons.features.dungeons.terminals

import noammaddons.noammaddons.Companion.config
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.GuiUtils.getPatcherScale
import java.awt.Color

object ConstantsVeriables {
    val MelodyTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &dMelody".addColor()
    val NumbersTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &9Numbers".addColor()
    val RubixTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &bRubix".addColor()
    val RedGreenTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &aRed &cGreen".addColor()
    val StartWithTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &6Starts With".addColor()
    val ColorsTitle = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r &2C&3o&4l&5o&6r".addColor()


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

    fun getTermScale(): Double = (config.CustomTerminalMenuScale * 4.0) / getPatcherScale()

    data class Slot(val num: Int, val id: Int, val meta: Int, val size: Int, val name: String, val enchanted: Boolean)
}