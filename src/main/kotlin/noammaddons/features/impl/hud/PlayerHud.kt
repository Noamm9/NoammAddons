package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.editgui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionBarParser
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.HudElementData
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.remove

object PlayerHud: Feature() {
    private class PlayerHudElement(
        data: HudElementData,
        private val isEnabled: () -> Boolean,
        private val textSupplier: () -> String,
    ): GuiElement(data) {
        override val enabled get() = isEnabled() && PlayerHud.enabled
        private val text get() = textSupplier()
        override val width: Float get() = getStringWidth(text)
        override val height: Float get() = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private val health = ToggleSetting("Health").register1()
    private val defense = ToggleSetting("Defense").register1()
    private val mana = ToggleSetting("Mana").register1()
    private val overflowMana = ToggleSetting("Overflow Mana").register1()
    private val hideIf0 = ToggleSetting("Hide if 0").addDependency(overflowMana).register1()
    private val ehp by ToggleSetting("Effective HP")
    private val speed by ToggleSetting("Speed")


    private val elements = listOf(
        PlayerHudElement(
            hudData.getData().playerHud.health,
            { health.value },
            { getHpFormatted() },
        ),
        PlayerHudElement(
            hudData.getData().playerHud.defense,
            { defense.value },
            { "&a${ActionBarParser.currentDefense}" },
        ),
        PlayerHudElement(
            hudData.getData().playerHud.mana,
            { mana.value },
            { "&b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}" },
        ),
        PlayerHudElement(
            hudData.getData().playerHud.overflowMana,
            { overflowMana.value && (! hideIf0.value || ActionBarParser.overflowMana > 0) },
            { "&3${ActionBarParser.overflowMana}" },
        ),
        PlayerHudElement(
            hudData.getData().playerHud.effectiveHP,
            { ehp },
            { "&2${format("${ActionBarParser.effectiveHP}")}" },
        ),
        PlayerHudElement(
            hudData.getData().playerHud.speed,
            { speed },
            { "&f${ActionBarParser.currentSpeed}✦" },
        )
    )

    @SubscribeEvent
    fun drawAll(event: RenderOverlay) {
        if (! inSkyblock) return
        elements.filter { it.enabled }.forEach { it.draw() }
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
        if (! enabled) return text
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
