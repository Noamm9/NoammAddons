package noammaddons.features.gui

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DrawSlotEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PlayerUtils.getArmor
import noammaddons.utils.RenderHelper.highlight


object SalvageOverlay: Feature() {
    @SubscribeEvent
    fun onDrawSlot(event: DrawSlotEvent) {
        if (! config.overlaySalvageable || ! inSkyblock) return
        if (getArmor()?.contains(event.slot.stack) == true) return
        if (event.slot.stack?.displayName?.contains("✪") == true) return
        val attributes = event.slot.stack?.getSubCompound("ExtraAttributes", false) ?: return
        if (! attributes.hasKey("baseStatBoostPercentage")) return
        if (attributes.hasKey("dungeon_item_level")) return
        event.slot.highlight(
            if (attributes.getInteger("baseStatBoostPercentage") == 50) config.overlayColorTopSalvageable
            else config.overlayColorSalvageable
        )
    }
}
