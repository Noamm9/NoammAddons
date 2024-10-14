package noammaddons.config

import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.features.hud.PlayerHud.PlayerHudData


class HudElementConfig {
    val BonzoMask = HudElementData(100f, 10f, 1f)
    val SpiritMask = HudElementData(100f, 20f, 1f)
    val PhoenixPet = HudElementData(100f, 30f, 1f)
    val GhostPick = HudElementData(100f, 40f, 1f)
    val ClockDisplay = HudElementData(100f, 50f, 1f)
    val FPSdisplay = HudElementData(100f, 60f, 1f)
    val WitherShieldTimer = HudElementData(100f, 70f, 2f)
    val SpringBootsDisplay = HudElementData(100f, 80f, 4f)
    val PlayerHud = PlayerHudData(
        health = HudElementData(100f, 90f, 1f),
        mana = HudElementData(100f, 100f, 1f),
        overflowMana = HudElementData(100f, 110f, 1f),
        defense = HudElementData(100f, 120f, 1f),
        effectiveHP = HudElementData(100f, 130f, 1f),
        speed = HudElementData(100f, 140f, 1f)
    )
	val PetDisplay = HudElementData(100f, 150f, 1f)
}