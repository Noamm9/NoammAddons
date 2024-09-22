package NoammAddons.features.cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ItemUtils.lore
import NoammAddons.utils.LocationUtils.inSkyblock
import NoammAddons.utils.PlayerUtils.sendRightClickAirPacket
import net.minecraft.item.ItemSword
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object NoBlockAnimation {
    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (!config.noBlockAnimation || !inSkyblock) return
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            val item = mc.thePlayer.heldItem ?: return
            if (item.item !is ItemSword) return

            if (mc.thePlayer.heldItem.lore.any { it.contains("§6Ability: ") && it.endsWith("§e§lRIGHT CLICK") }) {
                event.isCanceled = true
                if (mc.gameSettings.keyBindUseItem.isKeyDown) {
                    sendRightClickAirPacket()
                }
            }
        }
    }
}