package noammaddons.features.hud

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.config.EditGui.HudElement
import noammaddons.events.RenderOverlay
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.ActionBarParser
import noammaddons.utils.ChatUtils.formatNumber
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PlayerHud {
	private val data = hudData.getData().PlayerHud
	
    private data class ElementConfig(
        val element: HudElement,
        val isEnabled: () -> Boolean,
        val setText: (HudElement) -> Unit
    )
	
	data class PlayerHudData(
        var health: HudElementData,
        var defense: HudElementData,
        var effectiveHP: HudElementData,
        var mana: HudElementData,
        var overflowMana: HudElementData,
        var speed: HudElementData
    )



    private val elements = listOf(
        ElementConfig(
            element =  HudElement("&c2222/4000", dataObj = data.health),
            isEnabled = { config.PlayerHUDHealth },
            setText = { it.setText("${if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "&e" else "&c"}${ActionBarParser.currentHealth}&f/&c${ActionBarParser.maxHealth} ${if (ActionBarParser.wand != null) "(${ActionBarParser.wand})" else ""}") }
        ),
        ElementConfig(
            element = HudElement("&a5040", dataObj = data.defense),
            isEnabled = { config.PlayerHUDDefense },
            setText = { it.setText("&a${ActionBarParser.currentDefense}") }
        ),
        ElementConfig(
            element = HudElement("&b2222/4000", dataObj = data.mana),
            isEnabled = { config.PlayerHUDMana },
            setText = { it.setText("&b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}") }
        ),
        ElementConfig(
            element = HudElement("&3600", dataObj = data.overflowMana),
            isEnabled = { config.PlayerHUDOverflowMana },
            setText = { it.setText("&3${ActionBarParser.overflowMana}") }
        ),
        ElementConfig(
            element = HudElement("&222675", dataObj = data.effectiveHP),
            isEnabled = { config.PlayerHUDEffectiveHP },
            setText = { it.setText("&2${formatNumber("${ActionBarParser.effectiveHP}")}") }
        ),
        ElementConfig(
            element =  HudElement("&f500✦", dataObj = data.speed),
            isEnabled = { config.PlayerHUDSpeed },
            setText = { it.setText("&f${ActionBarParser.currentSpeed}✦") }
        )
    )

    @SubscribeEvent
    fun drawAll(event: RenderOverlay) {
        if (!config.PlayerHUD) return
        if (!inSkyblock) return

        elements.forEach { config ->
            if (config.isEnabled()) {
                config.setText(config.element)
                config.element.draw()
            }
        }
    }

    private val patterns = listOf(
        "(§.\\d{1,3}(,\\d{3})*\\/\\d{1,3}(,\\d{3})*[?❤]?)\\s+(§a\\d{1,3}(,\\d{3})*§a[?❈❤]? Defense)\\s+(§b\\d{1,3}(,\\d{3})*\\/\\d{1,3}(,\\d{3})*([?❤✎])?\\s+§3\\d+([?ʬ])?)".toRegex(),
        "§b[\\d,]+\\/[\\d,]+(\\?|✎ Mana)?( §3\\d+(\\?|ʬ))?".toRegex(),
        "[\\d|,]+§a❈ Defense".toRegex(),
        "[\\d|,]+/[\\d|,]+❤".toRegex(),
        "[\\d|,]+/[\\d|,]+✎ Mana".toRegex(),
        "[\\d|,]+/[\\d|,]+ Mana".toRegex(),
        "(§3\\d+(\\?|ʬ))?".toRegex(),
        "✎".toRegex()
    )
	
	/**
	 * @see noammaddons.mixins.MixinGuiIngame
	 */
    fun modifyText(text: String): String {
        if (!config.PlayerHUD || !inSkyblock) return text
        var result = text
        patterns.forEach { result = result.replace(it, "") }
        return result
    }
}

