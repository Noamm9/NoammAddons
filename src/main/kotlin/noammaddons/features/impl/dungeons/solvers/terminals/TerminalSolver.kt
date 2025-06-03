package noammaddons.features.impl.dungeons.solvers.terminals

import gg.essential.elementa.utils.withAlpha
import net.minecraftforge.common.MinecraftForge
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.solvers.terminals.core.ClickMode
import noammaddons.features.impl.dungeons.solvers.terminals.impl.*
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.Utils.favoriteColor
import java.awt.Color


object TerminalSolver: Feature() {
    private val terms = listOf(
        Colors, Melody, Numbers,
        RedGreen, Rubix, StartWith
    )

    val scale = SliderSetting("Scale", 1, 100, 1, 75)
    val clickMode = DropdownSetting("Mode", listOf("Normal", "Q-Terms"/*, "Hover Terms"*/))

    val reSyncTimeout = SliderSetting("Resync Timeout", 400, 1000, 50, 600)

    val solutionColor = ColorSetting("Solution Color", favoriteColor.withAlpha(170))
    val backgroundColor = ColorSetting("Background Color", Color(33, 33, 33, 195))

    val melody = ToggleSetting("Melody")
    val startWith = ToggleSetting("Start With")
    val rubix = ToggleSetting("Rubix")
    val numbers = ToggleSetting("Numbers")
    val redGreen = ToggleSetting("Red Green")
    val colors = ToggleSetting("Colors")

    override fun init() = addSettings(
        scale, clickMode, reSyncTimeout,
        SeperatorSetting("Terminal Colors"),
        solutionColor, backgroundColor,
        SeperatorSetting("Terminals"),
        melody, startWith, rubix,
        numbers, redGreen, colors
    )

    override fun onEnable() {
        super.onEnable()
        terms.forEach(MinecraftForge.EVENT_BUS::register)
    }

    override fun onDisable() {
        super.onDisable()
        terms.forEach(MinecraftForge.EVENT_BUS::unregister)
    }

    private const val prefix = "&6&l&n[&b&l&nN&d&l&nA&6&l&n] &b&l&nT&d&l&ne&b&l&nr&d&l&nm&b&l&ni&d&l&nn&b&l&na&d&l&nl&r:&r"

    val melodyTitle = "$prefix &dMelody".addColor()
    val numbersTitle = "$prefix &9Numbers".addColor()
    val rubixTitle = "$prefix &bRubix".addColor()
    val redGreenTitle = "$prefix &aRed &cGreen".addColor()
    val startWithTitle = "$prefix &6Starts With".addColor()
    val colorsTitle = "$prefix &2C&3o&4l&5o&6r".addColor()

    var lastClick = System.currentTimeMillis()

    fun getColorMode(): Color = backgroundColor.value

    fun getSolutionColor(): Color = solutionColor.value

    fun getClickMode(): ClickMode = ClickMode.entries[clickMode.value]

    fun getTermScale(): Float = ((scale.value.toFloat() / 100f) * 9f) / mc.getScaleFactor()

    val reSyncTime get() = reSyncTimeout.value.toLong()

    fun checkLastClick() = System.currentTimeMillis() - lastClick > 500
}