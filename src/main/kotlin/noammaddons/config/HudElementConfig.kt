package noammaddons.config

import noammaddons.noammaddons.Companion.mc
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import noammaddons.utils.ThreadUtils.runEvery
import noammaddons.features.hud.PlayerHud.PlayerHudData
import kotlin.properties.Delegates


class HudElementConfig {
    private var ScreenWidth = mc.getWidth() * 2f
	private var ScreenHeight = mc.getHeight() * 2f
	
	
    val BonzoMask = HudElementData(100f, 10f, 1f)
    val SpiritMask = HudElementData(100f, 20f, 1f)
    val PhoenixPet = HudElementData(100f, 30f, 1f)
    val GhostPick = HudElementData(100f, 40f, 1f)
    val ClockDisplay = HudElementData(100f, 50f, 1f)
    val FPSdisplay = HudElementData(100f, 60f, 1f)
    val WitherShieldTimer = HudElementData(ScreenWidth/2, ScreenHeight/2 + 30, 2f)
    val SpringBootsDisplay = HudElementData(ScreenWidth/2, ScreenHeight/4, 4f)
    val PlayerHud = PlayerHudData(
        health = HudElementData(100f, 10f, 1f),
        mana = HudElementData(100f, 20f, 1f),
        overflowMana = HudElementData(100f, 30f, 1f),
        defense = HudElementData(100f, 40f, 1f),
        effectiveHP = HudElementData(100f, 50f, 1f),
        speed = HudElementData(100f, 70f, 1f)
    )
	val PetDisplay = HudElementData(100f, 80f, 1f)
}