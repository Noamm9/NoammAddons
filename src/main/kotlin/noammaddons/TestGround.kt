package noammaddons

import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.*
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ItemUtils.rune
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.PlayerUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf


// used to be a place for me to test shit.
// but now it's just a dump of silent features/fixes

object TestGround {
    private var fuckingBitch = false
    private var sent = false
    private var a = false

    @SubscribeEvent
    fun handlePartyCommands(event: MessageSentEvent) {
        if (! onHypixel) return
        val msg = event.message.removeFormatting().lowercase()

        if (msg == "/p invite accept") {
            event.isCanceled = true
            a = true
            sent = false
            setTimeout(250) { a = false }
            return
        }

        if (a && ! sent) {
            if (msg.startsWith("/p invite ") || msg.startsWith("/party accept ")) {
                event.isCanceled = true

                val modifiedMessage = msg
                    .replace("/party accept ", "/p join ")
                    .replace("/p invite ", "/p join ")

                sendChatMessage(modifiedMessage)
                sent = true
            }
        }
    }

    @SubscribeEvent
    fun wtf(event: WorldLoadPostEvent) {
        setTimeout(500) {
            if (mc.currentScreen !is GuiDownloadTerrain) return@setTimeout
            mc.currentScreen = null
        }
    }

    @SubscribeEvent
    fun fucklocraw(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        if (event.isLocal) return
        fuckingBitch = true
        setTimeout(5000) { fuckingBitch = false }
    }

    @SubscribeEvent
    fun fuckLocraw2(event: PacketEvent.Sent) {
        val packet = event.packet as? C01PacketChatMessage ?: return
        if (fuckingBitch) {
            if (packet.message != "/locraw") return
            debugMessage("Cancelling /locraw")
            event.isCanceled = true
        }

        if (event.packet.message == "/odingetpingcommand-----") {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun dragonSpawn(event: PacketEvent.Received) {
        val packet = event.packet as? S0FPacketSpawnMob ?: return
        if (packet.entityType != 63) return
    }

    @SubscribeEvent
    fun onPacketReceived(event: PacketEvent.Received) {
        val packet = event.packet as? S2APacketParticles ?: return
        if (! packet.particleType.equalsOneOf(EnumParticleTypes.DRIP_WATER, EnumParticleTypes.CLOUD)) return
        val vec = Vec3(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate)
        if (vec.squareDistanceTo(mc.thePlayer?.positionVector ?: return) >= 16) return
        if (PlayerUtils.getHelmet().rune != "RAINY_DAY") return
        event.isCanceled = true
    }
}