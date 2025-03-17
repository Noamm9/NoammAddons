package noammaddons.features.alerts

import net.minecraft.network.play.server.S44PacketWorldBorder
import net.minecraft.world.border.WorldBorder
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.SoundUtils.ayaya
import noammaddons.utils.ThreadUtils.setTimeout

object ShadowAssassinAlert: Feature() {
    @SubscribeEvent
    fun saAlert(event: PacketEvent.Received) {
        if (event.packet !is S44PacketWorldBorder) return
        if (! inDungeon || ! config.ShadowAssassinAlert) return

        val bonzo = config.bonzoBossRespawnAlert && dungeonFloorNumber == 1
        val border = WorldBorder()
        event.packet.func_179788_a(border) // why is that func not mapped ):
        if (border.diameter != 1.0) return


        ayaya.start()
        setTimeout(300) { ayaya.start() }
        setTimeout(600) { ayaya.start() }
        showTitle(
            "",
            if (bonzo) "&cBonzo Respawn"
            else "&8Shadow Assassin",
            2f
        )
    }
}
