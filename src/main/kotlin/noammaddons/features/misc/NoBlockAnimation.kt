package noammaddons.features.misc

import net.minecraft.item.ItemSword
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.blackList
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import noammaddons.utils.Utils.equalsOneOf


object NoBlockAnimation: Feature() {
    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (! config.noBlockAnimation) return
        if (! inSkyblock) return
        if (! event.action.equalsOneOf(
                PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                PlayerInteractEvent.Action.RIGHT_CLICK_AIR
            )
        ) return
        val item = mc.thePlayer?.heldItem ?: return
        if (item.item !is ItemSword) return
        if (item.lore.none { it.contains("§6Ability: ") && it.endsWith("§e§lRIGHT CLICK") }) return
        if (! mc.gameSettings.keyBindUseItem.isKeyDown) return
        if (event.pos != null) {
            if (getBlockAt(event.pos) in blackList) return
        }

        event.isCanceled = true
        sendRightClickAirPacket()
    }
}
