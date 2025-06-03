package noammaddons.features.impl.alerts

import net.minecraft.network.play.server.S44PacketWorldBorder
import net.minecraft.world.border.WorldBorder
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout

object ShadowAssassin: Feature() {
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! inDungeon) return
        val packet = event.packet as? S44PacketWorldBorder ?: return
        val border = WorldBorder()
        packet.func_179788_a(border) // onAction
        if (border.diameter != 1.0) return

        SoundUtils.ayaya()
        setTimeout(300) { SoundUtils.ayaya() }
        setTimeout(600) { SoundUtils.ayaya() }
        showTitle(subtitle = if (dungeonFloorNumber == 1) "&cBonzo Respawn" else "&8Shadow Assassin")
    }
}