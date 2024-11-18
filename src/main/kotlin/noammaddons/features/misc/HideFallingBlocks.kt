package noammaddons.features.misc

import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inDungeons


object HideFallingBlocks : Feature() {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet is S0EPacketSpawnObject && event.packet.type == 70 && config.hideFallingBlocks && inDungeons)
            event.isCanceled = true
    }
}