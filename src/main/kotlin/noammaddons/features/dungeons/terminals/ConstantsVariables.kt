package noammaddons.features.dungeons.terminals

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.RenderHelper.getScaleFactor
import java.awt.Color

object ConstantsVariables {
    private const val prefix = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r"

    val MelodyTitle = "$prefix &dMelody".addColor()
    val NumbersTitle = "$prefix &9Numbers".addColor()
    val RubixTitle = "$prefix &bRubix".addColor()
    val RedGreenTitle = "$prefix &aRed &cGreen".addColor()
    val StartWithTitle = "$prefix &6Starts With".addColor()
    val ColorsTitle = "$prefix &2C&3o&4l&5o&6r".addColor()


    val lightmode = Color(203, 202, 205, 195)
    val darkmode = Color(33, 33, 33, 195)

    fun getColorMode(): Color = if (config.CustomTerminalMenuLightMode) lightmode else darkmode

    fun getSolutionColor(): Color = config.CustomTerminalMenuSolutionColor

    /**Todo(getClickMode)
     *  Maybe someday not feeling like making auto terms but hover might be cool
     * 0 = NORMAL
     * 1 = HOVER
     * 2 = AUTO
     */
    fun getClickMode(): Int = config.CustomTerminalMenuClickMode

    fun getTermScale(): Float = (config.CustomTerminalMenuScale * 9f) / mc.getScaleFactor()

    data class TerminalSlot(val num: Int, val id: Int, val meta: Int, val size: Int, val name: String, val enchanted: Boolean)
}

