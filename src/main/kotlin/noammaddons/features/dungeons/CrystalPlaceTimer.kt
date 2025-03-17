package noammaddons.features.dungeons

import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.MathUtils
import noammaddons.utils.NumbersUtils.toFixed

object CrystalPlaceTimer: Feature() {
    val regex = Regex("^(\\w+) picked up an Energy Crystal!$")
    var crystalTime = personalBests.getData().crystals
    var pickupTime: Long? = null


    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.CrystalPlaceTimer) return
        regex.find(event.component.noFormatText)?.destructured?.component1()?.let {
            if (it != mc.session.username) return
            pickupTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! config.CrystalPlaceTimer) return
        if (pickupTime == null) return
        val packet = event.packet as? S0FPacketSpawnMob ?: return
        if (packet.entityType != 200) return // Ender Crystal
        if (packet.y != 224) return
        val spawnPos = BlockPos(packet.x, packet.y, packet.z).toVec()
        val distance = MathUtils.distance2D(mc.thePlayer.positionVector, spawnPos)
        if (distance >= 5) return

        val placeTime = ((System.currentTimeMillis() - pickupTime !!) / 1000.0)
        var msg = "&aCrystal placed in &e${placeTime.toFixed(3)}s&a."
        if (placeTime < (crystalTime ?: Double.MAX_VALUE) || crystalTime == null) {
            crystalTime = placeTime
            personalBests.save()
            msg += " &d&l(PB)"
        }

        clickableChat(
            msg,
            prefix = true,
            hover = "&dPersonal Best: &a${crystalTime?.toFixed(3)}s",
        )
    }

    @SubscribeEvent
    fun onPacket(event: WorldUnloadEvent) {
        if (! config.CrystalPlaceTimer) return
        if (pickupTime == null) return
        pickupTime = null
    }
}
