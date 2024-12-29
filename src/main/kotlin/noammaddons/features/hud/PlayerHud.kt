package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ActionBarParser
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.NumbersUtils.format

object PlayerHud: Feature() {
    private val data = hudData.getData().PlayerHud

    private data class ElementConfig(
        val element: TextElement,
        val isEnabled: () -> Boolean,
        val updateText: (TextElement) -> Unit
    )

    data class PlayerHudData(
        var health: HudElementData,
        var defense: HudElementData,
        var effectiveHP: HudElementData,
        var mana: HudElementData,
        var overflowMana: HudElementData,
        var speed: HudElementData
    )

    private val elements = mutableListOf<ElementConfig>()

    init {
        listOf(
            ElementConfig(
                TextElement("&c2222/4000", dataObj = data.health),
                { config.PlayerHUDHealth },
                { it.setText(getHpFormatted()) }
            ),
            ElementConfig(
                TextElement("&a5040", dataObj = data.defense),
                { config.PlayerHUDDefense },
                { it.setText("&a${ActionBarParser.currentDefense}") }
            ),
            ElementConfig(
                TextElement("&b2222/4000", dataObj = data.mana),
                { config.PlayerHUDMana },
                { it.setText("&b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}") }
            ),
            ElementConfig(
                TextElement("&3600", dataObj = data.overflowMana),
                {
                    val cfg = config.PlayerHUDOverflowMana
                    val alternate = config.PlayerHUDAlternateOverflowMana
                    cfg && (alternate && ActionBarParser.overflowMana > 0 || ! alternate)
                },
                { it.setText("&3${ActionBarParser.overflowMana}") }
            ),
            ElementConfig(
                TextElement("&222675", dataObj = data.effectiveHP),
                { config.PlayerHUDEffectiveHP },
                { it.setText("&2${format("${ActionBarParser.effectiveHP}")}") }
            ),
            ElementConfig(
                TextElement("&f500✦", dataObj = data.speed),
                { config.PlayerHUDSpeed },
                { it.setText("&f${ActionBarParser.currentSpeed}✦") }
            )
        ).run { elements.addAll(this) }
    }

    @SubscribeEvent
    fun drawAll(event: RenderOverlay) {
        if (! config.PlayerHUD || ! inSkyblock) return

        elements.forEach { config ->
            if (config.isEnabled()) {
                config.updateText(config.element)
                config.element.draw()
            }
        }
    }

    private fun getHpFormatted(): String {
        var str = if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "&e" else "&c"
        str += "${ActionBarParser.currentHealth}&f/&c${ActionBarParser.maxHealth} "
        str += ActionBarParser.wand?.let { "(${it})" } ?: ""

        return str
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

