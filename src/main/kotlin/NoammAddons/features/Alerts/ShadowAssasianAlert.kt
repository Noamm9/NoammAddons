package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.Sounds.AYAYA
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.events.ReceivePacketEvent
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.ThreadUtils.setTimeout
import net.minecraft.network.play.server.S44PacketWorldBorder
import net.minecraft.world.border.WorldBorder

object ShadowAssasianAlert {
    @SubscribeEvent
    fun saAlert(event: ReceivePacketEvent) {
        if (event.packet !is S44PacketWorldBorder) return
        if (!inDungeons || !config.ShadowAssassinAlert) return

        val border = WorldBorder()
        event.packet.func_179788_a(border) // why is that func is not mapped ):

        if (border.getDiameter() == 1.0) {
            AYAYA.play()
            setTimeout(800) { AYAYA.play()}
            setTimeout(1600) { AYAYA.play()}
        }

    }

}
