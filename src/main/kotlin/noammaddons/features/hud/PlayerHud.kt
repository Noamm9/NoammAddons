package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ActionBarParser
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DataClasses
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.remove

object PlayerHud: Feature() {
    private class PlayerHudElement(
        data: DataClasses.HudElementData,
        private val isEnabled: () -> Boolean,
        private val textSupplier: () -> String,
    ): GuiElement(data) {
        override val enabled get() = isEnabled() && config.PlayerHUD
        private val text get() = textSupplier()
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private val elements = listOf(
        PlayerHudElement(
            hudData.getData().PlayerHud.health,
            { config.PlayerHUDHealth },
            { getHpFormatted() },
        ),
        PlayerHudElement(
            hudData.getData().PlayerHud.defense,
            { config.PlayerHUDDefense },
            { "&a${ActionBarParser.currentDefense}" },
        ),
        PlayerHudElement(
            hudData.getData().PlayerHud.mana,
            { config.PlayerHUDMana },
            { "&b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}" },
        ),
        PlayerHudElement(
            hudData.getData().PlayerHud.overflowMana,
            { config.PlayerHUDOverflowMana && (! config.PlayerHUDAlternateOverflowMana || ActionBarParser.overflowMana > 0) },
            { "&3${ActionBarParser.overflowMana}" },
        ),
        PlayerHudElement(
            hudData.getData().PlayerHud.effectiveHP,
            { config.PlayerHUDEffectiveHP },
            { "&2${format("${ActionBarParser.effectiveHP}")}" },
        ),
        PlayerHudElement(
            hudData.getData().PlayerHud.speed,
            { config.PlayerHUDSpeed },
            { "&f${ActionBarParser.currentSpeed}✦" },
        )
    )

    @SubscribeEvent
    fun drawAll(event: RenderOverlay) {
        if (! config.PlayerHUD || ! inSkyblock) return

        elements.forEach { element ->
            if (element.enabled) {
                element.draw()
            }
        }
    }

    private fun getHpFormatted(): String {
        var str = if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "&e" else "&c"
        str += "${ActionBarParser.currentHealth}&f/&c${ActionBarParser.maxHealth} "
        return str
    }

    private val patterns = mapOf(
        ActionBarParser.HP_REGEX to { elements[0].enabled },
        ActionBarParser.DEF_REGEX to { elements[1].enabled },
        ActionBarParser.MANA_REGEX to { elements[2].enabled },
        ActionBarParser.OVERFLOW_REGEX to { elements[3].enabled },
    )


    /**
     * @see noammaddons.mixins.MixinGuiIngame.modifyActionBar
     */
    @JvmStatic
    fun modifyText(text: String): String {
        if (! config.PlayerHUD) return text
        if (! inSkyblock) return text
        var result = text

        patterns.forEach { (pattern, condition) ->
            if (condition.invoke()) {
                result = result.remove(pattern)
            }
        }
        return result
    }

    @JvmStatic
    fun cancelActionBar(msg: String) = msg.removeFormatting().trim().isBlank()
}
