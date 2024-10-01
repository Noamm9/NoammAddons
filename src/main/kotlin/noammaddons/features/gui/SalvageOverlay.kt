package noammaddons.features.gui

import net.minecraft.client.gui.Gui
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.events.GuiContainerEvent.DrawSlotEvent
import noammaddons.utils.LocationUtils.inSkyblock

object SalvageOverlay {
    @SubscribeEvent
    fun onDrawSlot(event: DrawSlotEvent) {
        if (!config.overlaySalvageable || !inSkyblock) return
        val attributes = event.slot.stack?.getSubCompound("ExtraAttributes", false) ?: return

        if (attributes.hasKey("baseStatBoostPercentage") && !attributes.hasKey("dungeon_item_level")) {
            val x = event.slot.xDisplayPosition
            val y = event.slot.yDisplayPosition

            Gui.drawRect(
                x, y, x + 16, y + 16,
                if (attributes.getInteger("baseStatBoostPercentage") == 50) {
                    config.overlayColorTopSalvageable.rgb
                }
                else config.overlayColorSalvageable.rgb
            )
        }
    }
}
