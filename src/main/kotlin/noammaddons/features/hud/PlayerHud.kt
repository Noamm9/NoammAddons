package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ActionBarParser
import noammaddons.utils.ChatUtils.formatNumber
import noammaddons.utils.LocationUtils.inSkyblock

object PlayerHud: Feature() {
    private val data = hudData.getData().PlayerHud

    private data class ElementConfig(
        val element: TextElement,
        val isEnabled: () -> Boolean,
        val setText: (TextElement) -> Unit
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
            element = TextElement("&c2222/4000", dataObj = data.health),
            isEnabled = { config.PlayerHUDHealth },
            setText = { it.setText("${if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "&e" else "&c"}${ActionBarParser.currentHealth}&f/&c${ActionBarParser.maxHealth} ${if (ActionBarParser.wand != null) "(${ActionBarParser.wand})" else ""}") }
        ),
        ElementConfig(
            element = TextElement("&a5040", dataObj = data.defense),
            isEnabled = { config.PlayerHUDDefense },
            setText = { it.setText("&a${ActionBarParser.currentDefense}") }
        ),
        ElementConfig(
            element = TextElement("&b2222/4000", dataObj = data.mana),
            isEnabled = { config.PlayerHUDMana },
            setText = { it.setText("&b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}") }
        ),
        ElementConfig(
            element = TextElement("&3600", dataObj = data.overflowMana),
            isEnabled = { config.PlayerHUDOverflowMana },
            setText = { it.setText("&3${ActionBarParser.overflowMana}") }
        ),
        ElementConfig(
            element = TextElement("&222675", dataObj = data.effectiveHP),
            isEnabled = { config.PlayerHUDEffectiveHP },
            setText = { it.setText("&2${formatNumber("${ActionBarParser.effectiveHP}")}") }
        ),
        ElementConfig(
            element = TextElement("&f500✦", dataObj = data.speed),
            isEnabled = { config.PlayerHUDSpeed },
            setText = { it.setText("&f${ActionBarParser.currentSpeed}✦") }
        )
    )

    @SubscribeEvent
    fun drawAll(event: RenderOverlay) {
        if (! config.PlayerHUD) return
        if (! inSkyblock) return

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
        if (! config.PlayerHUD || ! inSkyblock) return text
        var result = text
        patterns.forEach { result = result.replace(it, "") }
        return result
    }
}

