package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.google.gson.reflect.TypeToken
import net.minecraft.world.phys.AABB
import java.awt.Color
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object PositionalMessages : Feature("Sends a party message when near a position. /posmsg") {
    private val onlyDungeons by ToggleSetting("Only in Dungeons", true)
    private val oncePerWorld by ToggleSetting("Once Per World", false)
    private val showPositions by ToggleSetting("Show Positions", true)
    private val renderDistance by SliderSetting("Render Distance", 64f, 16f, 128f, 16f)

    data class PosMessage(
        val x: Double, val y: Double, val z: Double,
        val x2: Double?, val y2: Double?, val z2: Double?,
        val delay: Double, val distance: Double?,
        val colorRgb: Int, val message: String
    ) {
        val color: Color get() = Color(colorRgb)
    }

    private val configFile = File("config/$MOD_NAME/positionalMessages.json")
    val posMessages: MutableList<PosMessage> = mutableListOf()
    private val sentMessages = mutableMapOf<PosMessage, Boolean>()

    override fun init() {
        loadConfig()

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
                        outline = true, fill = false, phase = false
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

    private fun loadConfig() {
        if (!configFile.exists()) return

        runCatching {
            FileReader(configFile).use { reader ->
                val type = object : TypeToken<MutableList<PosMessage>>() {}.type
                val loaded = JsonUtils.gsonBuilder.fromJson<MutableList<PosMessage>>(reader, type)
                if (loaded != null) {
                    posMessages.clear()
                    posMessages.addAll(loaded)
                    NoammAddons.logger.info("PositionalMessages: Loaded ${posMessages.size} messages.")
                }
            }
        }.onFailure {
            NoammAddons.logger.error("PositionalMessages: Failed to load config!", it)
        }
    }

    fun saveConfig() {
        runCatching {
            configFile.parentFile?.mkdirs()
            FileWriter(configFile).use { writer ->
                JsonUtils.gsonBuilder.toJson(posMessages, writer)
            }
            NoammAddons.logger.info("PositionalMessages: Config saved.")
        }.onFailure {
            NoammAddons.logger.error("PositionalMessages: Failed to save config!", it)
        }
    }

    private fun handleAtMessage(msg: PosMessage) {
        val player = mc.player ?: return
        val sent = sentMessages.getOrDefault(msg, false)

        if (player.distanceToSqr(msg.x, msg.y, msg.z) <= (msg.distance ?: return) * msg.distance) {
            if (!sent) {
                sentMessages[msg] = true
                val delayTicks = (msg.delay * 20).toInt()
                if (delayTicks <= 0) {
                    ChatUtils.sendCommand("pc ${msg.message}")
                } else {
                    ThreadUtils.scheduledTask(delayTicks) {
                        if (mc.player?.distanceToSqr(msg.x, msg.y, msg.z) ?: Double.MAX_VALUE <= msg.distance * msg.distance)
                            ChatUtils.sendCommand("pc ${msg.message}")
                    }
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
                val delayTicks = (msg.delay * 20).toInt()
                if (delayTicks <= 0) {
                    ChatUtils.sendCommand("pc ${msg.message}")
                } else {
                    ThreadUtils.scheduledTask(delayTicks) {
                        if (box.contains(mc.player?.position() ?: return@scheduledTask))
                            ChatUtils.sendCommand("pc ${msg.message}")
                    }
                }
            }
        } else if (!oncePerWorld.value) {
            sentMessages[msg] = false
        }
    }
}