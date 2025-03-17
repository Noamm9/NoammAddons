package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.ClickEvent
import noammaddons.features.Feature
import noammaddons.utils.ActionBarParser.currentHealth
import noammaddons.utils.ActionBarParser.maxHealth
import noammaddons.utils.ActionBarParser.overflowMana
import noammaddons.utils.ItemUtils.SkyblockID

object BlockGloomlockOverUse: Feature() {
    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (! config.blockGloomlockOverUse) return
        if (mc.thePlayer?.heldItem?.SkyblockID != "GLOOMLOCK_GRIMOIRE") return
        event.isCanceled = currentHealth > maxHealth * 0.8
    }

    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (! config.blockGloomlockOverUse) return
        if (mc.thePlayer?.heldItem?.SkyblockID != "GLOOMLOCK_GRIMOIRE") return
        event.isCanceled = overflowMana == 600 || currentHealth < maxHealth * 0.3
    }
}
