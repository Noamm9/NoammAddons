package com.github.noamm9.features.impl.general

//#if CHEAT

import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.item.Items

object CancelInteract: Feature("Disables Hypixel's stupid Ender Pearl throw block when you are looking at a wall/floor/ceiling.") {
    override fun init() {
        register<PacketEvent.Sent> {
            if (event.packet !is ServerboundUseItemOnPacket) return@register
            val itemStack = mc.player?.getItemInHand(event.packet.hand) ?: return@register
            if (itemStack.item != Items.ENDER_PEARL) return@register
            event.isCanceled = true
        }
    }
}
//#endif