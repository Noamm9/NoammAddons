package noammaddons.features.misc

import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature


object HideFallingBlocks: Feature() {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! config.hideFallingBlocks) return
        if (event.packet !is S0EPacketSpawnObject) return
        if (event.packet.type != 70) return
        event.isCanceled = true
    }
}