package NoammAddons.config

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.config.EditGui.ElementsManager.HudElementData
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth


class HudElementConfig {
    private val ScreenWidth = mc.getWidth().toDouble()
    private val ScreenHeight = mc.getHeight().toDouble()

    val BonzoMask = HudElementData(10.0, 10.0, 1.0)
    val SpiritMask = HudElementData(10.0, 20.0, 1.0)
    val PhoenixPet = HudElementData(10.0, 30.0, 1.0)
    val GhostPick = HudElementData(10.0, 40.0, 1.0)
    val ClockDisplay = HudElementData(10.0, 50.0, 1.0)
    val FPSdisplay = HudElementData(10.0, 60.0, 1.0)
    val WitherShieldTimer = HudElementData(ScreenWidth/2, ScreenHeight/2 + 30, 2.0)
    val SpringBootsDisplay = HudElementData(ScreenWidth/2, ScreenHeight/4, 4.0)
}