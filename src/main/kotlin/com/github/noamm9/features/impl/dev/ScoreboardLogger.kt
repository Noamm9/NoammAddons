package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import gg.essential.universal.UDesktop
import net.minecraft.network.protocol.game.*
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ScoreboardLogger: Feature("Logs every scoreboard text update to a file.") {
    private val onlyDungeons by ToggleSetting("Only In Dungeons", true).withDescription("Only logs while inside a dungeon.")
    private val withFormatting by ToggleSetting("With formatting").withDescription("With Color code formatting.")
    private val logMode by DropdownSetting("Log Mode", 0, listOf("Changed Lines", "Full Snapshot")).withDescription("Changed Lines only logs lines that update, Full Snapshot logs the whole scoreboard on every change.")
    private val openFolder by ButtonSetting("Open Logs Folder") {
        logDir.mkdirs()
        UDesktop.browse(logDir.toURI())
    }.withDescription("Opens the folder where scoreboard logs are saved.")

    private val logDir = FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).resolve("logs").toFile()
    private val fileStamp = DateTimeFormatter.ofPattern("dd_HH-mm-ss")

    private var writer: BufferedWriter? = null
    private var lastSnapshot: List<String>? = null

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (onlyDungeons.value && ! LocationUtils.inDungeon) return@register
            if (updatePackets.none { it.java.isInstance(event.packet) }) return@register
            val snapshot = snapshot() ?: return@register
            val previous = lastSnapshot
            if (snapshot == lastSnapshot) return@register
            lastSnapshot = snapshot

            if (logMode.value == 0) log(previous ?: emptyList(), snapshot)
            else write(snapshot)
        }

        register<WorldChangeEvent> { reset() }
        ThreadUtils.addShutdownHook { reset() }
    }

    override fun onDisable() {
        super.onDisable()
        reset()
    }

    private fun snapshot(): List<String>? {
        val scoreboard = mc.level?.scoreboard ?: return null
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null

        val lines = mutableListOf(objective.displayName.let { if (withFormatting.value) it.formattedText else it.unformattedText })
        scoreboard.listPlayerScores(objective).sortedByDescending { it.value }.take(15).forEach { score ->
            val name = score.ownerName().string
            lines.add(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(name), Component.literal(name)).let { if (withFormatting.value) it.formattedText else it.unformattedText })
        }
        return lines
    }

    private fun log(previous: List<String>, current: List<String>) {
        val max = maxOf(previous.size, current.size)
        val changes = buildList {
            for (i in 0 until max) {
                val old = previous.getOrNull(i)
                val new = current.getOrNull(i)
                if (old == new) continue
                add("${i + 1}. ${new ?: "(removed)"}")
            }
        }
        write(changes)
    }

    private fun write(lines: List<String>) {
        val writer = getWriter() ?: return

        runCatching {
            writer.write("== ${LocalDateTime.now()} ==\n")
            lines.forEach { writer.write("  $it\n") }
            writer.write("\n")
            writer.flush()
        }.onFailure { logger.error("ScoreboardLogger: failed to write log", it) }
    }

    private fun getWriter(): BufferedWriter? {
        writer?.let { return it }
        return runCatching {
            logDir.mkdirs()
            val file = File(logDir, "scoreboard_${LocalDateTime.now().format(fileStamp)}.log")
            BufferedWriter(FileWriter(file, true)).also {
                writer = it
            }
        }.onFailure { logger.error("ScoreboardLogger: failed to open log file", it) }.getOrNull()
    }

    private fun reset() {
        runCatching { writer?.close() }
        writer = null
        lastSnapshot = null
    }

    private val updatePackets = setOf(
        ClientboundSetScorePacket::class, ClientboundSetObjectivePacket::class,
        ClientboundSetDisplayObjectivePacket::class, ClientboundResetScorePacket::class,
        ClientboundSetPlayerTeamPacket::class
    )
}