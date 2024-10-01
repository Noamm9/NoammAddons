package noammaddons.config

import noammaddons.noammaddons.Companion.mc
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import noammaddons.utils.ThreadUtils.runEvery
import noammaddons.features.hud.PlayerHud.PlayerHudData


class HudElementConfig {
    private var ScreenWidth = mc.getWidth().toDouble()
    private var ScreenHeight = mc.getHeight().toDouble()

    @Suppress("unused")
    private val getFullScreenSize = runEvery(1000, { mc.isFullScreen }) {
        ScreenWidth = mc.getWidth() * 2.0
        ScreenHeight = mc.getHeight() * 2.0
    }

    val BonzoMask = HudElementData(100.0, 10.0, 1.0)
    val SpiritMask = HudElementData(100.0, 20.0, 1.0)
    val PhoenixPet = HudElementData(100.0, 30.0, 1.0)
    val GhostPick = HudElementData(100.0, 40.0, 1.0)
    val ClockDisplay = HudElementData(100.0, 50.0, 1.0)
    val FPSdisplay = HudElementData(100.0, 60.0, 1.0)
    val WitherShieldTimer = HudElementData(ScreenWidth/2, ScreenHeight/2 + 30, 2.0)
    val SpringBootsDisplay = HudElementData(ScreenWidth/2, ScreenHeight/4, 4.0)
    val PlayerHud = PlayerHudData(
        health = HudElementData(100.0, 10.0, 1.0),
        mana = HudElementData(100.0, 20.0, 1.0),
        overflowMana = HudElementData(100.0, 30.0, 1.0),
        defense = HudElementData(100.0, 40.0, 1.0),
        effectiveHP = HudElementData(100.0, 50.0, 1.0),
        speed = HudElementData(100.0, 70.0, 1.0)
    )
	val PetDisplay = HudElementData(100.0, 80.0, 1.0)
}