package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.config
import noammaddons.events.PacketEvent
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.LocationUtils.inDungeons


object HideFallingBlocks {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet is S0EPacketSpawnObject && event.packet.type == 70 && config.hideFallingBlocks && inDungeons)
            event.isCanceled = true
    }
}