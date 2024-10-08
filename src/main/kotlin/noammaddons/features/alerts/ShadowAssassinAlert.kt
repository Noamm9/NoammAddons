package noammaddons.features.alerts

import noammaddons.noammaddons.Companion.config
import noammaddons.sounds.AYAYA
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.ThreadUtils.setTimeout
import net.minecraft.network.play.server.S44PacketWorldBorder
import net.minecraft.world.border.WorldBorder
import noammaddons.utils.ChatUtils.showTitle

object ShadowAssassinAlert {
    @SubscribeEvent
    fun saAlert(event: PacketEvent.Received) {
        if (event.packet !is S44PacketWorldBorder) return
        if (!inDungeons || !config.ShadowAssassinAlert) return

        val border = WorldBorder()
        event.packet.func_179788_a(border) // why is that func is not mapped ):

        if (border.diameter == 1.0) {
            AYAYA.play()
            setTimeout(300) { AYAYA.play()}
            setTimeout(600) { AYAYA.play()}
            showTitle("", "&8Shadow Assassin", 2f)
        }
    }
}
