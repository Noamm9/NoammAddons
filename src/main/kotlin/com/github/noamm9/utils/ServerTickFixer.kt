package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.TickEvent
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.*
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket

// fallback server ticks for when the server stops sending pings
// each ~50ms packet burst counts as a tick, corrected by ClientboundSetTimePacket gameTime every second
object ServerTickFixer {
    private const val BURST_WINDOW = 38_000_000L
    private const val TICK_NANOS = 50_000_000L
    private const val MAX_CORRECTION = 100L

    private var lastBurst = 0L
    private var lastPing = 0L
    private var anchorGameTime = - 1L
    private var postedTicks = 0L
    private var pendingSkips = 0L
    private var usingPings = true

    @JvmStatic
    fun onPacket(packet: Packet<*>) {
        val now = System.nanoTime()

        when (packet) {
            is ClientboundLoginPacket, is ClientboundRespawnPacket -> return reset()
            is ClientboundKeepAlivePacket, is ClientboundSystemChatPacket, is ClientboundPongResponsePacket -> return
            is ClientboundPingPacket -> if (packet.id != 0) {
                lastPing = now
                anchorGameTime = - 1L
                if (! usingPings) {
                    usingPings = true
                }
                return postTicks(1)
            }
        }

        if (now - lastPing < 1_000_000_000L) return
        if (usingPings) {
            usingPings = false
            logger.info("[ServerTickFixer] falling back to packet bursts + SetTime for server ticks")
        }

        if (now - lastBurst > BURST_WINDOW) {
            val ticks = if (lastBurst == 0L) 1L else ((now - lastBurst) / TICK_NANOS.toDouble() + 0.4).toLong().coerceAtLeast(1)
            lastBurst = now
            val skipped = minOf(ticks, pendingSkips)
            pendingSkips -= skipped
            postTicks(ticks - skipped)
        }

        if (packet is ClientboundSetTimePacket) correct(packet.gameTime)
    }

    private fun correct(gameTime: Long) {
        if (anchorGameTime == - 1L) {
            anchorGameTime = gameTime
            postedTicks = 0L
            pendingSkips = 0L
            return
        }

        val deficit = (gameTime - anchorGameTime) - postedTicks
        if (deficit !in - MAX_CORRECTION .. MAX_CORRECTION) return reset()

        pendingSkips = (- deficit).coerceAtLeast(0)
        if (deficit > 0) postTicks(deficit)
    }

    private fun postTicks(count: Long) {
        if (count <= 0) return
        if (anchorGameTime != - 1L) postedTicks += count
        mc.execute { repeat(count.toInt()) { EventBus.post(TickEvent.Server) } }
    }

    private fun reset() {
        lastBurst = 0L
        anchorGameTime = - 1L
        postedTicks = 0L
        pendingSkips = 0L
    }
}
