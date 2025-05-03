package noammaddons.features.impl.dungeons.solvers.terminals

import gg.essential.elementa.utils.withAlpha
import net.minecraftforge.common.MinecraftForge
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.Utils.favoriteColor
import java.awt.Color


object TerminalSolver: Feature() {
    val terms = listOf(
        StartWith, Rubix,
        Numbers, RedGreen,
        Melody, Colors
    )

    val scale = SliderSetting("Scale", 1, 100, 75)
    val clickMode = DropdownSetting("Mode", listOf("Default", "Q-Terms", "Hover Terms"))

    val reSyncTimeout = SliderSetting("Resync Timeout", 400, 1000, 600)

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

    var lastClick = System.currentTimeMillis()
}