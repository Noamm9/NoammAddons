package noammaddons.features.impl.gui

import gg.essential.elementa.utils.withAlpha
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DrawSlotEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PlayerUtils.getArmor
import noammaddons.utils.RenderHelper.highlight
import java.awt.Color


object SalvageOverlay: Feature() {
    private val under50 by ColorSetting("Hightlight Color", Color.cyan.withAlpha(160))
    private val base50 by ColorSetting("50% stats Color", Color.RED.withAlpha(160))

    @SubscribeEvent
    fun onDrawSlot(event: DrawSlotEvent) {
        if (! inSkyblock) return
        if (getArmor()?.contains(event.slot.stack) == true) return
        if (event.slot.stack?.displayName?.contains("✪") == true) return
        val attributes = event.slot.stack?.getSubCompound("ExtraAttributes", false) ?: return
        if (! attributes.hasKey("baseStatBoostPercentage")) return
        if (attributes.hasKey("dungeon_item_level")) return
        event.slot.highlight(
            if (attributes.getInteger("baseStatBoostPercentage") == 50) base50
            else under50
        )
    }
}
