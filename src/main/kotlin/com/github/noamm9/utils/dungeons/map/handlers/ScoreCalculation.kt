package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.impl.dungeon.ScoreCalculator
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.Utils.containsOneOf
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.websocket.WebSocket
import com.github.noamm9.websocket.packets.S2CPacketDungeonMimic
import com.github.noamm9.websocket.packets.S2CPacketDungeonScore
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.entity.monster.Zombie
import kotlin.jvm.optionals.getOrNull
import kotlin.math.floor

object ScoreCalculation {
    private val secretsFoundPattern = Regex(" Secrets Found: §b(?<secrets>\\d+)")
    private val secretsFoundPercentagePattern = Regex(" Secrets Found: §[ae](?<percentage>[\\d.]+)%")
    private val cryptsPattern = Regex(" Crypts: §6(?<crypts>\\d+)")
    private val completedRoomsRegex = Regex(" Completed Rooms: §d(?<count>\\d+)")
    private val dungeonClearedPattern = Regex("Cleared: (?<percentage>\\d+)% \\(\\d+\\)")
    private val timeElapsedPattern = Regex(" Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")

    private var bloodDone = false

    private var had300 = false
        set(value) {
            field = value
            if (value) EventBus.post(DungeonEvent.Score(300))
        }

    private var had270 = false
        set(value) {
            field = value
            if (value) EventBus.post(DungeonEvent.Score(270))
        }

    var mimicKilled = false
    var princeKilled = false
    var deathCount = 0
    var foundSecrets = 0
    var cryptsCount = 0
    var secretPercentage = 0.0
    var clearedPercentage = 0
    var completedRooms = 0
    var secondsElapsed = 0

    var score = 0
        private set

    private val totalRooms: Int
        get() = if (completedRooms > 0 && clearedPercentage > 0)
            floor((completedRooms / (clearedPercentage / 100.0)) + 0.4).toInt()
        else 36

    private fun recalculate() {
        if (! DungeonListener.dungeonStarted) return
        val currentFloor = LocationUtils.dungeonFloor ?: return

        var bScore = cryptsCount.coerceAtMost(5)
        if (mimicKilled && (LocationUtils.dungeonFloorNumber ?: 0) > 5) bScore += 2
        if (princeKilled) bScore += 1
        if (DungeonUtils.isPaul()) bScore += 10
        val bonusScore = bScore

        val limit = timeLimit[currentFloor] ?: 100
        val speedScore = if (secondsElapsed <= limit) 100
        else (100 - getSpeedDeduction(((secondsElapsed - limit) * 100f / limit))).toInt().coerceAtLeast(0)

        val effectiveCompletedRooms = completedRooms + (if (! bloodDone) 1 else 0) + (if (! LocationUtils.inBoss) 1 else 0)

        val reqSecret = requiredSecretPercentage[currentFloor] ?: 1.0
        val secretsScore = floor((secretPercentage / reqSecret) / 100.0 * 40.0).coerceIn(0.0, 40.0).toInt()

        val completedRoomScore = (effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 60.0).coerceIn(0.0, 60.0).toInt()
        val skillRooms = floor(effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 80f).coerceIn(0.0, 80.0).toInt()

        val puzzlePenalty = DungeonListener.puzzles.count { ! it.state.equalsOneOf(RoomState.GREEN, RoomState.CLEARED) } * 10
        val deathPenalty = (deathCount * 2 - 1).coerceAtLeast(0)

        score = secretsScore + completedRoomScore + (20 + skillRooms - puzzlePenalty - deathPenalty).coerceIn(20, 100) + bonusScore + speedScore

        if (score >= 270 && ! had270) had270 = true
        else if (score >= 300 && ! had300) had300 = true
    }

    fun onPacket(packet: Packet<*>) {
        if (packet is ClientboundPlayerInfoUpdatePacket) {
            if (! packet.actions().containsOneOf(UPDATE_DISPLAY_NAME, ADD_PLAYER)) return
            packet.entries().forEach { entry ->
                val line = entry.displayName?.formattedText ?: return@forEach

                when {
                    line.contains("Crypts:") -> cryptsPattern.find(line)?.let {
                        cryptsCount = it.groups["crypts"]?.value?.toIntOrNull() ?: cryptsCount
                    }

                    line.contains("Completed Rooms:") -> completedRoomsRegex.find(line)?.let {
                        completedRooms = it.groups["count"]?.value?.toIntOrNull() ?: completedRooms
                    }

                    line.contains("Secrets Found:") -> if (line.contains('%')) secretsFoundPercentagePattern.find(line)?.let {
                        secretPercentage = it.groups["percentage"]?.value?.toDoubleOrNull() ?: secretPercentage
                    }
                    else secretsFoundPattern.find(line)?.let {
                        foundSecrets = it.groups["secrets"]?.value?.toIntOrNull() ?: foundSecrets
                    }
                }

                recalculate()
            }
        }
        else if (packet is ClientboundSetPlayerTeamPacket) {
            val prams = packet.parameters.getOrNull() ?: return
            val text = (prams.playerPrefix.string + prams.playerSuffix.string).removeFormatting()

            when {
                text.startsWith("Cleared:") -> dungeonClearedPattern.find(text)?.let {
                    val newCompletedRooms = it.groups["percentage"]?.value?.toIntOrNull()
                    if (newCompletedRooms != clearedPercentage && DungeonListener.watcherClearTime != null) bloodDone = true
                    clearedPercentage = newCompletedRooms ?: clearedPercentage
                }

                text.startsWith("Time Elapsed:") -> timeElapsedPattern.find(text)?.let { matcher ->
                    val hours = matcher.groups["hrs"]?.value?.toIntOrNull() ?: 0
                    val minutes = matcher.groups["min"]?.value?.toIntOrNull() ?: 0
                    val seconds = matcher.groups["sec"]?.value?.toIntOrNull() ?: 0
                    secondsElapsed = (hours * 3600 + minutes * 60 + seconds)
                }
            }
        }
        else if (packet is ClientboundEntityEventPacket) {
            if (packet.eventId.toInt() != 3) return
            if ((LocationUtils.dungeonFloorNumber ?: 0) < 6 || LocationUtils.inBoss) return
            if ((packet.getEntity(mc.level !!) as? Zombie)?.isBaby != true) return
            mimicKilled = true

            if (DungeonListener.dungeonTeammatesNoSelf.isNotEmpty()) {
                WebSocket.send(S2CPacketDungeonMimic())
            }

            if (ScoreCalculator.enabled && ScoreCalculator.sendMimic.value) {
                ChatUtils.sendPartyMessage("Mimic Killed")
            }
        }
        else if (packet is ClientboundSystemChatPacket) {
            if (packet.overlay()) return
            val msg = packet.content.unformattedText.lowercase()

            if (! princeKilled && princeMessages.any { msg.contains(it) }) {
                princeKilled = true

                if (DungeonListener.dungeonTeammatesNoSelf.isNotEmpty()) {
                    WebSocket.send(S2CPacketDungeonScore())
                }

                if (ScoreCalculator.enabled && ScoreCalculator.sendPrince.value && msg == "a prince falls. +1 bonus score") {
                    ChatUtils.sendPartyMessage("Prince Killed")
                }
            }

            if ((LocationUtils.dungeonFloorNumber ?: 0) > 5 && ! LocationUtils.inBoss && mimicMessages.any { msg.contains(it) }) {
                mimicKilled = true
            }
        }
    }

    fun reset() {
        bloodDone = false
        had300 = false
        had270 = false
        mimicKilled = false
        princeKilled = false
        deathCount = 0
        foundSecrets = 0
        cryptsCount = 0
        secretPercentage = .0
        clearedPercentage = 0
        completedRooms = 0
        secondsElapsed = 0
        score = 0
    }

    private fun getSpeedDeduction(percentage: Float): Float {
        var percentageOver = percentage
        var deduction = 0f

        fun dedu(cap: Float, div: Float) {
            if (percentageOver <= 0) return
            deduction += (percentageOver.coerceAtMost(cap) / div)
            percentageOver -= cap
        }

        dedu(20f, 2f)
        dedu(20f, 3.5f)
        dedu(10f, 4f)
        dedu(10f, 5f)

        if (percentageOver > 0) deduction += (percentageOver / 6f)

        return deduction
    }

    private val requiredSecretPercentage = mapOf(
        "E" to 0.3, "F1" to 0.3, "F2" to 0.4, "F3" to 0.5, "F4" to 0.6,
        "F5" to 0.7, "F6" to 0.85, "F7" to 1.0, "M1" to 1.0, "M2" to 1.0,
        "M3" to 1.0, "M4" to 1.0, "M5" to 1.0, "M6" to 1.0, "M7" to 1.0
    )

    private val timeLimit = mapOf(
        "E" to 600, "F1" to 600, "F2" to 600, "F3" to 600, "F4" to 720,
        "F5" to 600, "F6" to 720, "F7" to 840, "M1" to 480, "M2" to 480,
        "M3" to 480, "M4" to 480, "M5" to 480, "M6" to 600, "M7" to 840
    )

    private val mimicMessages = setOf(
        "mimic dead!", "mimic dead", "mimic killed!", "mimic killed",
        "\$skytils-dungeon-score-mimic$", "child destroyed!", "mimic obliterated!",
        "mimic exorcised!", "mimic destroyed!", "mimic annhilated!",
        "breefing killed", "breefing dead"
    )

    private val princeMessages = setOf(
        "prince dead", "prince dead!", "\$skytils-dungeon-score-prince$",
        "prince killed", "prince slain", "prince killed!",
        "a prince falls. +1 bonus score"
    )
}