package noammaddons.features.impl.misc

import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.ServerPlayer
import noammaddons.utils.Utils.send


object NoBlockAnimation: Feature() {
    private var isRightClickKeyDown = false

    @SubscribeEvent
    fun onTick(event: Tick) {
        isRightClickKeyDown = mc.gameSettings.keyBindUseItem.isKeyDown
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR) return
        val item = ServerPlayer.player.getHeldItem()?.takeIf { it.item !is ItemSword } ?: return
        if (item.lore.none { it.contains("§6Ability: ") && it.endsWith("§e§lRIGHT CLICK") }) return
        if (! isRightClickKeyDown) C08PacketPlayerBlockPlacement(ServerPlayer.player.getHeldItem()).send()
        event.isCanceled = true
    }
}
