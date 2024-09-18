package NoammAddons.features.cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.PacketEvent
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TimeChanger {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet is S03PacketTimeUpdate && config.TimeChanger && mc.theWorld != null) {
            event.isCanceled = true
            mc.theWorld.worldTime = when (config.TimeChangerMode) {
                0 -> 1000 // Day
                1 -> 6000 // Noon
                2 -> 12000 // Sunset
                3 -> 13000 // Night
                4 -> 18000 // Midnight
                5 -> 23000 // Sunrise
                else -> 0
            }
        }
    }
}
