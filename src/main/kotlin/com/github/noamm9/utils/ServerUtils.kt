package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import net.minecraft.util.Util
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket
import kotlin.math.min

object ServerUtils {
    var tps = 20f
        private set

    var currentPing = 0L
        private set

    var averagePing = 0L
        private set

    private var lastTimePacket = 0L
    private var pingStartTime = 0L
    private var isPinging = false
    private var tickCounter = 0

    fun init() {
        register<WorldChangeEvent>(EventPriority.HIGHEST) {
            tps = 20f
            currentPing = 0
            averagePing = 0
            lastTimePacket = 0L
            isPinging = false
        }

        register<TickEvent.Start>(EventPriority.HIGHEST) {
            if (isPinging && Util.getNanos() - pingStartTime > 10_000_000_000L) {
                isPinging = false
            }

            tickCounter ++
            if (tickCounter >= 80) {
                tickCounter = 0
                sendPingRequest()
            }
        }

        register<PacketEvent.Received>(EventPriority.HIGHEST) {
            val packet = event.packet

            if (event.packet is ClientboundSetTimePacket) {
                val now = System.currentTimeMillis()
                if (lastTimePacket != 0L) {
                    val diff = now - lastTimePacket
                    tps = (20_000f / diff).coerceIn(0f, 20f)
                }
                lastTimePacket = now
            }
            else if (packet is ClientboundPongResponsePacket) {
                currentPing = (Util.getMillis() - packet.time).coerceAtLeast(0L)
                isPinging = false

                val pingLog = mc.debugOverlay.pingLogger
                val sampleSize = min(pingLog.size().toInt(), 10)

                if (sampleSize > 0) {
                    var total = 0L
                    for (i in 0 until sampleSize) total += pingLog.get(i)
                    averagePing = total / sampleSize
                }
                else averagePing = currentPing
            }
        }
    }

    private fun sendPingRequest() {
        if (isPinging || mc.player == null) return

        val connection = mc.connection ?: return
        isPinging = true
        pingStartTime = Util.getNanos()
        connection.send(ServerboundPingRequestPacket(Util.getMillis()))
    }
}
