package noammaddons.features.gui

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiContainerEvent.DrawSlotEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PlayerUtils.getArmor
import noammaddons.utils.RenderUtils.drawRoundedRect


object SalvageOverlay: Feature() {
    @SubscribeEvent
    fun onDrawSlot(event: DrawSlotEvent) {
        if (! config.overlaySalvageable || ! inSkyblock) return
        if (getArmor()?.contains(event.slot.stack) == true) return
        val attributes = event.slot.stack?.getSubCompound("ExtraAttributes", false) ?: return
        if (attributes.hasKey("baseStatBoostPercentage") && ! attributes.hasKey("dungeon_item_level")) {
            drawRoundedRect(
                if (attributes.getInteger("baseStatBoostPercentage") == 50) config.overlayColorTopSalvageable
                else config.overlayColorSalvageable,
                event.slot.xDisplayPosition,
                event.slot.yDisplayPosition,
                16, 16, 0
            )
        }
    }
}
