package noammaddons.features.dungeons

import net.minecraft.init.Items
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.ServerPlayer

object StonkSwapSound: Feature() {
    // If the item index you are holding is not the same as the item the index the server think you are holding then you stonk swapped.
    // Also, don't make a sound if you swapped from a bow because it does not work with bows for some reason work.
    @SubscribeEvent
    fun onBlockBreak(event: PacketEvent.Sent) {
        if (! config.stonkSwapSound) return
        val packet = event.packet as? C07PacketPlayerDigging ?: return
        if (packet.status != C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) return
        if (ServerPlayer.player.heldHotbarSlot == mc.thePlayer.inventory.currentItem) return
        if (mc.thePlayer.heldItem?.item == Items.bow) return
        config.playStonkSwapSound()
    }
}
