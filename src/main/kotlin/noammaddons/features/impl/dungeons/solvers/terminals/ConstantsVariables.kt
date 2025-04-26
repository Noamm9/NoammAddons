package noammaddons.features.impl.dungeons.solvers.terminals

import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver.lastClick
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.RenderHelper.getScaleFactor
import java.awt.Color

object ConstantsVariables {
    data class TerminalSlot(val num: Int, val id: Int, val meta: Int, val size: Int, val name: String, val enchanted: Boolean)
    private const val prefix = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r"

    val MelodyTitle = "$prefix &dMelody".addColor()
    val NumbersTitle = "$prefix &9Numbers".addColor()
    val RubixTitle = "$prefix &bRubix".addColor()
    val RedGreenTitle = "$prefix &aRed &cGreen".addColor()
    val StartWithTitle = "$prefix &6Starts With".addColor()
    val ColorsTitle = "$prefix &2C&3o&4l&5o&6r".addColor()

    fun getColorMode(): Color = TerminalSolver.backgroundColor.value

    fun getSolutionColor(): Color = TerminalSolver.solutionColor.value

    fun getClickMode(): Int = TerminalSolver.clickMode.value

    fun getTermScale(): Float = ((TerminalSolver.scale.value.toFloat() / 100f) * 9f) / mc.getScaleFactor()

    fun getResyncTime(): Long = TerminalSolver.reSyncTimeout.value.toLong()

    fun checkLastClick() = System.currentTimeMillis() - lastClick > 85

}

