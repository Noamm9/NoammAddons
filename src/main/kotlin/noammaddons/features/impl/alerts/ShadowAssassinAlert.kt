package noammaddons.features.impl.alerts

import net.minecraft.network.play.server.S44PacketWorldBorder
import net.minecraft.world.border.WorldBorder
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout

object ShadowAssassinAlert: Feature() {
    override fun init() = onPacket<S44PacketWorldBorder>({ inDungeon }) { packet ->
        val border = WorldBorder()
        packet.func_179788_a(border)
        if (border.diameter != 1.0) return@onPacket

        SoundUtils.ayaya()
        setTimeout(300) { SoundUtils.ayaya() }
        setTimeout(600) { SoundUtils.ayaya() }
        showTitle(subtitle = if (dungeonFloorNumber == 1) "&cBonzo Respawn" else "&8Shadow Assassin")
    }
}