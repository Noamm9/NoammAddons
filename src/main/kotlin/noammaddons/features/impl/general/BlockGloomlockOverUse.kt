package noammaddons.features.impl.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.ClickEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionBarParser.currentHealth
import noammaddons.utils.ActionBarParser.maxHealth
import noammaddons.utils.ActionBarParser.overflowMana
import noammaddons.utils.ItemUtils.SkyblockID


object BlockGloomlockOverUse: Feature("Stops you from right/left clicking gloomlock more than you should") {
    private val blockRightClick by ToggleSetting("Block Right Click", true)
    private val blockLeftClick by ToggleSetting("Block Left Click", true)

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (! blockRightClick) return
        if (mc.thePlayer?.heldItem?.SkyblockID != "GLOOMLOCK_GRIMOIRE") return
        event.isCanceled = currentHealth > maxHealth * 0.8
    }

    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (! blockLeftClick) return
        if (mc.thePlayer?.heldItem?.SkyblockID != "GLOOMLOCK_GRIMOIRE") return
        event.isCanceled = overflowMana == 600 || currentHealth < maxHealth * 0.3
    }
}
