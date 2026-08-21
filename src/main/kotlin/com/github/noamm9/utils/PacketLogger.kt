package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Appends every payload ModHider refuses to send to `config/NoammAddons/logs/blocked_packets.log`,
 * so you can see exactly what a server asked for and whitelist it if it was a false positive.
 */
object PacketLogger {
    private const val MAX_BYTES = 5L * 1024 * 1024
    private const val SEPARATOR = "--------------------------------------------------"
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val logDir = FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).resolve("logs").toFile()
    private val logFile = File(logDir, "blocked_packets.log")

    fun logBlocked(source: String, payload: CustomPacketPayload) = log(source, "Blocked Packet", payload)

    fun log(source: String, action: String, payload: CustomPacketPayload) {
        val timestamp = LocalDateTime.now().format(dateFormat)
        val id = payload.type().id().toString()
        val contents = payload.toString()

        ThreadUtils.async {
            runCatching {
                logDir.mkdirs()
                trimIfNeeded()
                logFile.appendText(
                    buildString {
                        appendLine("[$timestamp] [$source] $action: $id")
                        appendLine("    Payload Content: $contents")
                        appendLine(SEPARATOR)
                    }
                )
            }.onFailure { NoammAddons.logger.error("[ModHider] Failed to log blocked packet", it) }
        }
    }

    private fun trimIfNeeded() {
        if (! logFile.exists() || logFile.length() <= MAX_BYTES) return

        val lines = logFile.readLines()
        var trimFrom = lines.size / 2
        while (trimFrom < lines.size && ! lines[trimFrom].startsWith("[")) trimFrom ++

        logFile.writeText(lines.drop(trimFrom).joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
        NoammAddons.logger.info("[ModHider] Trimmed blocked_packets.log")
    }
}
