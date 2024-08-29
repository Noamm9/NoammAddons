package NoammAddons.features.Cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.events.ReceivePacketEvent
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.utils.LocationUtils.inDungeons


object HideFallingBlocks {
    @SubscribeEvent
    fun onPacket(event: ReceivePacketEvent) {
        if (event.packet is S0EPacketSpawnObject && event.packet.type == 70 && config.hideFallingBlocks && inDungeons)
            event.isCanceled = true
    }
}