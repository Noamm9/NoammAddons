package NoammAddons.config

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.config.EditGui.ElementsManager.HudElementData
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import NoammAddons.utils.ThreadUtils.runEvery
import NoammAddons.features.hud.PlayerHud.PlayerHudData


class HudElementConfig {
    private var ScreenWidth = mc.getWidth().toDouble()
    private var ScreenHeight = mc.getHeight().toDouble()

    @Suppress("unused")
    val getFullScreenSize = runEvery(1000, { mc.isFullScreen }) {
        ScreenWidth = mc.getWidth().toDouble()
        ScreenHeight = mc.getHeight().toDouble()
    }

    val BonzoMask = HudElementData(10.0, 10.0, 1.0)
    val SpiritMask = HudElementData(10.0, 20.0, 1.0)
    val PhoenixPet = HudElementData(10.0, 30.0, 1.0)
    val GhostPick = HudElementData(10.0, 40.0, 1.0)
    val ClockDisplay = HudElementData(10.0, 50.0, 1.0)
    val FPSdisplay = HudElementData(10.0, 60.0, 1.0)
    val WitherShieldTimer = HudElementData(ScreenWidth/2, ScreenHeight/2 + 30, 2.0)
    val SpringBootsDisplay = HudElementData(ScreenWidth/2, ScreenHeight/4, 4.0)
    val PlayerHud = PlayerHudData(
        health = HudElementData(100.0, 10.0, 1.0),
        mana = HudElementData(100.0, 20.0, 1.0),
        overflowMana = HudElementData(100.0, 30.0, 1.0),
        defense = HudElementData(100.0, 40.0, 1.0),
        effectiveHP = HudElementData(100.0, 50.0, 1.0),
        speed = HudElementData(100.0, 60.0, 1.0)
    )
}