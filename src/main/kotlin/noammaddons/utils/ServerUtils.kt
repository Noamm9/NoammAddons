package noammaddons.utils

import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S37PacketStatistics
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.PacketEvent
import noammaddons.events.Tick
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.Utils.sendNoEvent
import java.util.*


object ServerUtils {
    var averageTps = 20.0
        private set

    var averagePing = 0
        private set
    var latestPing = 0
        private set

    private val pingHistory = LinkedList<Int>()
    private var pingStartTime = 0L
    private var isPinging = false
    private var tickCounter = 0
    private var prevTpsTime = 0L

    @SubscribeEvent
    fun onClientConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        reset()
    }

    @SubscribeEvent
    fun onClientDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        reset()
    }

    @SubscribeEvent
    fun onClientTick(event: Tick) {
        if (isPinging && System.nanoTime() - pingStartTime > 10_000_000_000L) isPinging = false

        tickCounter ++
        if (tickCounter >= 80) {
            tickCounter = 0
            sendPingRequest()
        }
    }

    @SubscribeEvent
    fun onPacketReceived(event: PacketEvent.Received) {
        if (event.packet is S37PacketStatistics && isPinging) {
            val rttNano = System.nanoTime() - pingStartTime
            val rttMillis = (rttNano / 1_000_000).toInt()

            latestPing = rttMillis
            updateAverage(rttMillis)

            isPinging = false
        }
        else if (event.packet is S03PacketTimeUpdate) {
            if (prevTpsTime != 0L) averageTps = (20_000.0 / (System.currentTimeMillis() - prevTpsTime + 1)).coerceIn(0.0, 20.0).toFixed(1).toDouble()
            prevTpsTime = System.currentTimeMillis()
        }
    }

    private fun sendPingRequest() {
        if (isPinging || mc.thePlayer == null) return

        isPinging = true
        pingStartTime = System.nanoTime()
        C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS).sendNoEvent()
    }

    private fun updateAverage(newPing: Int) {
        pingHistory.add(newPing)
        if (pingHistory.size > 5) pingHistory.removeFirst()
        averagePing = if (pingHistory.isNotEmpty()) pingHistory.sum() / pingHistory.size else 0
    }

    private fun reset() {
        isPinging = false
        pingStartTime = 0L
        tickCounter = 0
        pingHistory.clear()
        latestPing = 0
        averagePing = 0
        prevTpsTime = 0
        averageTps = 20.0
    }
}