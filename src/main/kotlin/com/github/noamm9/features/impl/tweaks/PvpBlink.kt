package com.github.noamm9.features.impl.tweaks

import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.componnents.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.showIf
import com.github.noamm9.ui.clickgui.componnents.withDescription
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render3D
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

object PvpBlink: Feature("Desyncs your connection to eat knockback or spoof position.") {
    private val mode by DropdownSetting("Mode", 0, listOf("Manual", "Auto", "Pulse"))
        .withDescription("Manual: Hold key. Auto: On Velocity. Pulse: Every 0.3s.")

    private val blinkDuration by SliderSetting("Blink Duration", 300.0, 50.0, 1000.0, 50.0)
        .withDescription("How long to desync (ms).")

    private val key by KeybindSetting("Blink Key", GLFW.GLFW_KEY_P)
        .showIf { mode.value != 0 }

    private var isBlinking = false
    private var isFlushing = false
    private var blinkStartTime = 0L
    private val sentQueue = ConcurrentLinkedQueue<Packet<*>>()

    private object ServerPlayer {
        var x = 0.0;
        var y = 0.0;
        var z = 0.0
        var yaw = 0f;
        var pitch = 0f
    }

    override fun init() {
        register<PacketEvent.Received> {
            if (mc.singleplayerServer != null) return@register
            if (mode.value == 1 && event.packet is ClientboundSetEntityMotionPacket) {
                if (event.packet.id == mc.player?.id) startBlink()
            }
        }

        register<PacketEvent.Sent> {
            if (mc.singleplayerServer != null) return@register
            if (isFlushing) return@register

            val packet = event.packet

            if (isBlinking) {
                if (packet is ServerboundInteractPacket) {
                    stopBlink()
                    return@register
                }

                sentQueue.add(packet)
                event.isCanceled = true
            }

            if (packet is ServerboundMovePlayerPacket && ! isBlinking) {
                val now = System.currentTimeMillis()
                val shouldStart = when (mode.value) {
                    0 -> key.isDown()
                    2 -> key.isDown() && (now - blinkStartTime > (blinkDuration.value + 100))
                    else -> false
                }
                if (shouldStart) startBlink()
            }

            if (packet is ServerboundMovePlayerPacket && ! event.isCanceled) {
                if (packet.hasPosition()) {
                    ServerPlayer.x = packet.getX(ServerPlayer.x)
                    ServerPlayer.y = packet.getY(ServerPlayer.y)
                    ServerPlayer.z = packet.getZ(ServerPlayer.z)
                }
                if (packet.hasRotation()) {
                    ServerPlayer.yaw = packet.getYRot(ServerPlayer.yaw)
                    ServerPlayer.pitch = packet.getXRot(ServerPlayer.pitch)
                }
            }
        }

        register<RenderWorldEvent> {
            if (mc.singleplayerServer != null) return@register
            if (isBlinking) {
                Render3D.renderBox(event.ctx, ServerPlayer.x, ServerPlayer.y, ServerPlayer.z, 0.6, 1.8, Color.RED.withAlpha(100))
            }
        }

        register<TickEvent.Start> {
            if (mc.singleplayerServer != null) return@register
            if (mode.value != 0) {
                if (isBlinking && System.currentTimeMillis() - blinkStartTime > blinkDuration.value.toLong()) {
                    stopBlink()
                }
            }
            else if (! key.isDown()) stopBlink()
        }
    }

    private fun startBlink() {
        if (mc.player == null || isBlinking || isFlushing) return
        isBlinking = true
        blinkStartTime = System.currentTimeMillis()
    }

    private fun stopBlink() {
        if (! isBlinking) return
        isBlinking = false

        isFlushing = true
        while (sentQueue.isNotEmpty()) {
            val p = sentQueue.poll() ?: continue
            mc.connection?.send(p)
        }
        isFlushing = false
    }

    override fun onDisable() {
        super.onDisable()
        stopBlink()
        sentQueue.clear()
    }
}