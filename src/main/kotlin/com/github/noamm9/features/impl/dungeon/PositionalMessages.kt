package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.world.phys.AABB
import java.awt.Color
import java.util.*
import kotlin.concurrent.schedule

object PositionalMessages : Feature("Sends a party message when near a position. /posmsg") {
    private val onlyDungeons by ToggleSetting("Only in Dungeons", true)
    private val oncePerWorld by ToggleSetting("Once Per World", false)
    private val showPositions by ToggleSetting("Show Positions", true)
    private val renderDistance by SliderSetting("Render Distance", 64f, 16f, 128f, 16f)

    data class PosMessage(
        val x: Double, val y: Double, val z: Double,
        val x2: Double?, val y2: Double?, val z2: Double?,
        val delay: Long, val distance: Double?,
        val color: Color, val message: String
    )

    val posMessages = mutableListOf<PosMessage>()
    private val sentMessages = mutableMapOf<PosMessage, Boolean>()

    override fun init() {
        register<WorldChangeEvent> {
            if (oncePerWorld.value) sentMessages.forEach { (msg, _) -> sentMessages[msg] = false }
        }

        register<TickEvent.Start> {
            if (onlyDungeons.value && !LocationUtils.inDungeon) return@register
            posMessages.forEach { message ->
                message.x2?.let { handleInMessage(message) } ?: handleAtMessage(message)
            }
        }

        register<RenderWorldEvent> {
            if (!showPositions.value) return@register
            if (onlyDungeons.value && !LocationUtils.inDungeon) return@register
            val player = mc.player ?: return@register

            posMessages.forEach { message ->
                val dist = if (message.distance != null) {
                    player.distanceToSqr(message.x, message.y, message.z)
                } else {
                    val centerX = (message.x + (message.x2 ?: message.x)) / 2
                    val centerY = (message.y + (message.y2 ?: message.y)) / 2
                    val centerZ = (message.z + (message.z2 ?: message.z)) / 2
                    player.distanceToSqr(centerX, centerY, centerZ)
                }
                val maxDist = renderDistance.value * renderDistance.value
                if (dist > maxDist) return@forEach

                if (message.distance != null) {
                    Render3D.renderBox(
                        event.ctx,
                        message.x, message.y, message.z,
                        message.distance * 2, 0.2,
                        message.color,
                        outline = true, fill = false, phase = !showPositions.value
                    )
                } else {
                    val box = AABB(
                        message.x, message.y, message.z,
                        message.x2 ?: return@forEach,
                        message.y2 ?: return@forEach,
                        message.z2 ?: return@forEach
                    )
                    Render3D.renderBox(
                        event.ctx,
                        (message.x + message.x2!!) / 2,
                        message.y,
                        (message.z + message.z2!!) / 2,
                        box.xsize, box.ysize,
                        message.color,
                        outline = true, fill = false, phase = false
                    )
                }
            }
        }
    }

    private fun handleAtMessage(msg: PosMessage) {
        val player = mc.player ?: return
        val sent = sentMessages.getOrDefault(msg, false)

        if (player.distanceToSqr(msg.x, msg.y, msg.z) <= (msg.distance ?: return)) {
            if (!sent) {
                sentMessages[msg] = true
                Timer().schedule(msg.delay) {
                    if (player.distanceToSqr(msg.x, msg.y, msg.z) <= msg.distance)
                        ChatUtils.sendCommand("pc ${msg.message}")
                }
            }
        } else if (!oncePerWorld.value) {
            sentMessages[msg] = false
        }
    }

    private fun handleInMessage(msg: PosMessage) {
        val pos = mc.player?.position() ?: return
        val sent = sentMessages.getOrDefault(msg, false)
        val box = AABB(msg.x, msg.y, msg.z, msg.x2 ?: return, msg.y2 ?: return, msg.z2 ?: return)

        if (box.contains(pos)) {
            if (!sent) {
                sentMessages[msg] = true
                Timer().schedule(msg.delay) {
                    if (box.contains(mc.player?.position() ?: return@schedule))
                        ChatUtils.sendCommand("pc ${msg.message}")
                }
            }
        } else if (!oncePerWorld.value) {
            sentMessages[msg] = false
        }
    }
}