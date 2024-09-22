package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.config.EditGui.ElementsManager.HudElementData
import NoammAddons.config.EditGui.HudElement
import NoammAddons.events.RenderOverlay
import NoammAddons.utils.LocationUtils.inSkyblock
import NoammAddons.utils.ActionBarParser
import NoammAddons.utils.ChatUtils.formatNumber
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PlayerHud {
    data class PlayerHudData(
        var health: HudElementData,
        var defense: HudElementData,
        var effectiveHP: HudElementData,
        var mana: HudElementData,
        var overflowMana: HudElementData,
        var speed: HudElementData
    )

    data class ElementConfig(
        val element: HudElement,
        val isEnabled: () -> Boolean,
        val setText: (HudElement) -> Unit
    )


    private val elements = listOf(
        ElementConfig(
            element =  HudElement("&c2222/4000", dataObj = hudData.getData().PlayerHud.health),
            isEnabled = { config.PlayerHUDHealth },
            setText = { it.setText("${if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "&e" else "&c"}${ActionBarParser.currentHealth}&f/&c${ActionBarParser.maxHealth} ${if (ActionBarParser.wand != null) "(${ActionBarParser.wand})" else ""}") }
        ),
        ElementConfig(
            element = HudElement("&a5040", dataObj = hudData.getData().PlayerHud.defense),
            isEnabled = { config.PlayerHUDDefense },
            setText = { it.setText("&a${ActionBarParser.currentDefense}") }
        ),
        ElementConfig(
            element = HudElement("&b2222/4000", dataObj = hudData.getData().PlayerHud.mana),
            isEnabled = { config.PlayerHUDMana },
            setText = { it.setText("&b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}") }
        ),
        ElementConfig(
            element = HudElement("&3600", dataObj = hudData.getData().PlayerHud.overflowMana),
            isEnabled = { config.PlayerHUDOverflowMana },
            setText = { it.setText("&3${ActionBarParser.overflowMana}") }
        ),
        ElementConfig(
            element = HudElement("&222675", dataObj = hudData.getData().PlayerHud.effectiveHP),
            isEnabled = { config.PlayerHUDEffectiveHP },
            setText = { it.setText("&2${formatNumber("${ActionBarParser.effectiveHP}")}") }
        ),
        ElementConfig(
            element =  HudElement("&f500✦", dataObj = hudData.getData().PlayerHud.speed),
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

    // @See MixinGuiIngame
    @Suppress("unused")
    fun modifyText(text: String): String {
        if (!config.PlayerHUD || !inSkyblock) return text
        var result = text
        patterns.forEach { result = result.replace(it, "") }
        return result
    }
}

