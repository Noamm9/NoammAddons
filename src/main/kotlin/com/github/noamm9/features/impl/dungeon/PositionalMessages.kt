package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.google.gson.reflect.TypeToken
import net.minecraft.core.BlockPos
import java.awt.Color
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.math.pow

object PositionalMessages: Feature("Sends a party message when near a position. /posmsg") {
    private val showPositions by ToggleSetting("Show Positions", true)
    private val renderDistance by SliderSetting("Render Distance", 64f, 16f, 128f, 16f)

    data class PosMessage(val pos: BlockPos, val delay: Double, val radius: Double, val color: Color, val message: String)

    private val configFile = File("config/$MOD_NAME/positionalMessages.json")
    val posMessages = mutableListOf<PosMessage>()
    private val sentMessages = mutableListOf<PosMessage>()

    override fun init() {
        loadConfig()

        register<WorldChangeEvent> { sentMessages.clear() }

        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon) return@register
            posMessages.forEach(::handleAtMessage)
        }

        register<RenderWorldEvent> {
            if (! showPositions.value) return@register
            if (! LocationUtils.inDungeon) return@register
            val player = mc.player ?: return@register

            posMessages.forEach { message ->
                val dist = player.blockPosition().distSqr(message.pos)
                val maxDist = renderDistance.value.pow(2)
                if (dist > maxDist) return@forEach

                Render3D.renderBox(
                    event.ctx,
                    message.pos.x, message.pos.y, message.pos.z,
                    message.radius * 2, 0.2,
                    message.color,
                    outline = true, fill = false, phase = false
                )
                Render3D.renderString(
                    message.message,
                    message.pos.x, message.pos.y + 1.5, message.pos.z,
                    message.color,
                    scale = 1f,
                    phase = false
                )
            }
        }
    }

    private fun handleAtMessage(msg: PosMessage) {
        val player = mc.player ?: return
        if (sentMessages.contains(msg)) return
        if (player.blockPosition().distSqr(msg.pos) > msg.radius.pow(2)) return
        sentMessages.add(msg)

        val delayTicks = (msg.delay * 20).toInt()
        if (delayTicks <= 0) ChatUtils.sendCommand("pc ${msg.message}")
        else ThreadUtils.scheduledTask(delayTicks) {
            if (player.blockPosition().distSqr(msg.pos) > msg.radius.pow(2)) return@scheduledTask
            ChatUtils.sendCommand("pc ${msg.message}")
        }
    }

    private fun loadConfig() {
        if (! configFile.exists()) return
        runCatching {
            FileReader(configFile).use { reader ->
                val type = object: TypeToken<MutableList<PosMessage>>() {}.type
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
}