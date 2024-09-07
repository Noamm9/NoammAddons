package NoammAddons.features.gui

import net.minecraft.client.gui.Gui
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.events.GuiContainerEvent.DrawSlotEvent
import NoammAddons.utils.LocationUtils.inSkyblock
import java.awt.Color

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
