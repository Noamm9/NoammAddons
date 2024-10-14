package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import net.minecraft.item.ItemSword
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.PlayerUtils.Player


// Bugs with the left click etherwarp - temporarily disabling
object NoBlockAnimation {/*
    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (!config.noBlockAnimation || !inSkyblock) return
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            val item = Player?.heldItem ?: return
            if (item.item !is ItemSword) return

            if (item.lore.any { it.contains("§6Ability: ") && it.endsWith("§e§lRIGHT CLICK") }) {
                event.isCanceled = true
                if (mc.gameSettings.keyBindUseItem.isKeyDown) {
                    sendRightClickAirPacket()
                }
            }
        }
    }*/
}
