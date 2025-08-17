package noammaddons.features.impl.dungeons.dmap.handlers

import gg.essential.elementa.state.BasicState
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3EPacketTeams
import noammaddons.events.PacketEvent
import noammaddons.features.impl.alerts.CryptsDone
import noammaddons.features.impl.dungeons.MimicDetector
import noammaddons.features.impl.dungeons.ScoreCalculator
import noammaddons.features.impl.dungeons.dmap.core.map.RoomState
import noammaddons.utils.DungeonUtils
import noammaddons.utils.DungeonUtils.puzzles
import noammaddons.utils.DungeonUtils.watcherClearTime
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.ScoreboardUtils
import noammaddons.utils.Utils.equalsOneOf
import kotlin.math.floor


object ScoreCalculation {
    private val secretsFoundPattern = Regex("§r Secrets Found: §r§b(?<secrets>\\d+)§r")
    private val secretsFoundPercentagePattern = Regex("§r Secrets Found: §r§[ae](?<percentage>[\\d.]+)%§r")
    private val cryptsPattern = Regex("§r Crypts: §r§6(?<crypts>\\d+)§r")
    private val completedRoomsRegex = Regex("§r Completed Rooms: §r§d(?<count>\\d+)§r")
    private val dungeonClearedPattern = Regex("Cleared: (?<percentage>\\d+)% \\(\\d+\\)")
    private val timeElapsedPattern = Regex(" Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")
    private var bloodDone = false

    var alerted300 = false
    var alerted270 = false

    var deathCount = 0
    var secretCount = 0
    var cryptsCount = BasicState(0)

    init {
        cryptsCount.onSetValue(CryptsDone.func)
    }

    var secretPercentage = 0.0
    var clearedPercentage = 0
    var completedRooms = 0
    var secondsElapsed = 0
    var mimicKilled = false
    var princeKilled = false


    private val totalRooms
        get() = if (completedRooms > 0 && clearedPercentage > 0)
            floor((completedRooms / (clearedPercentage / 100.0)) + 0.4).toInt()
        else 36

    val score: Int
        get() {
            val currentFloor = dungeonFloor ?: return 0
            val effectiveCompletedRooms = completedRooms + (if (! bloodDone) 1 else 0) + (if (! inBoss) 1 else 0)

            val secretsScore = floor((secretPercentage / (requiredSecretPercentage[currentFloor] !!)) / 100.0 * 40.0).coerceIn(.0, 40.0).toInt()
            val completedRoomScore = (effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 60.0).coerceIn(.0, 60.0).toInt()

            val skillRooms = floor(effectiveCompletedRooms.toDouble() / totalRooms.toDouble() * 80f).coerceIn(.0, 80.0).toInt()
            val puzzlePenalty = (puzzles.size - puzzles.count { it.state == RoomState.GREEN }) * 10
            val deathPenalty = (deathCount * 2 - 1).coerceAtLeast(0)

            val score = secretsScore + completedRoomScore + (20 + skillRooms - puzzlePenalty - deathPenalty).coerceIn(20, 100) + bonusScore + speedScore

            if (score >= 270 && ! alerted270) ScoreCalculator.on270Score()
            if (score >= 300 && ! alerted300) ScoreCalculator.on300Score()
            return score
        }

    val bonusScore: Int
        get() {
            var score = cryptsCount.get().coerceAtMost(5)
            if (MimicDetector.mimicKilled.get() && (dungeonFloorNumber ?: 0) > 5) score += 2
            if (MimicDetector.princeKilled.get()) score += 1
            if (DungeonUtils.isPaul()) score += 10
            return score
        }

    val speedScore: Int
        get() {
            val limit = timeLimit[dungeonFloor] ?: return 100
            if (secondsElapsed <= limit) return 100
            val percentageOver = (secondsElapsed - limit) * 100f / limit
            return (100 - getSpeedDeduction(percentageOver)).toInt().coerceAtLeast(0)
        }


    fun onWorldUnload() {
        deathCount = 0
        secretCount = 0
        cryptsCount.set(0)
        secretPercentage = 0.0
        clearedPercentage = 0
        completedRooms = 0
        secondsElapsed = 0
        mimicKilled = false
        princeKilled = false
        alerted300 = false
        alerted270 = false
        bloodDone = false
    }


    fun onPacket(event: PacketEvent.Received) {
        when (event.packet) {
            is S38PacketPlayerListItem -> {
                if (! event.packet.action.equalsOneOf(S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, S38PacketPlayerListItem.Action.ADD_PLAYER)) return
                val tabListEntries = event.packet.entries?.mapNotNull { it?.displayName?.formattedText } ?: return
                updateFromTab(tabListEntries)
            }

            is S3EPacketTeams -> {
                if (event.packet.action != 2) return
                val text = event.packet.prefix?.plus(event.packet.suffix) ?: return
                updateFromScoreboard(ScoreboardUtils.cleanSB(text))
            }
        }
    }


    fun updateFromTab(tabListEntries: List<String>) {
        tabListEntries.forEach { line ->
            when {
                line.contains("Crypts:") -> cryptsPattern.find(line)?.let { cryptsCount.set(it.groups["crypts"]?.value?.toIntOrNull() ?: cryptsCount.get()) }
                line.contains("Completed Rooms:") -> completedRoomsRegex.find(line)?.let {
                    completedRooms = it.groups["count"]?.value?.toIntOrNull() ?: completedRooms
                }

                line.contains("Secrets Found:") -> if (line.contains('%')) secretsFoundPercentagePattern.find(line)?.let {
                    secretPercentage = it.groups["percentage"]?.value?.toDoubleOrNull() ?: secretPercentage
                }
                else secretsFoundPattern.find(line)?.let { secretCount = it.groups["secrets"]?.value?.toIntOrNull() ?: secretCount }
            }
        }
    }

    fun updateFromScoreboard(line: String) = when {
        line.startsWith("Cleared:") -> dungeonClearedPattern.find(line)?.let {
            val newCompletedRooms = it.groups["percentage"]?.value?.toIntOrNull()
            if (newCompletedRooms != clearedPercentage && watcherClearTime != null) bloodDone = true
            clearedPercentage = newCompletedRooms ?: clearedPercentage
        }

        line.startsWith("Time Elapsed:") -> timeElapsedPattern.find(line)?.let { matcher ->
            val hours = matcher.groups["hrs"]?.value?.toIntOrNull() ?: 0
            val minutes = matcher.groups["min"]?.value?.toIntOrNull() ?: 0
            val seconds = matcher.groups["sec"]?.value?.toIntOrNull() ?: 0
            secondsElapsed = (hours * 3600 + minutes * 60 + seconds)
        }

        else -> {}
    }


    private fun getSpeedDeduction(percentage: Float): Float {
        var percentageOver = percentage
        var deduction = 0f
        deduction += (percentageOver.coerceAtMost(20f) / 2f).also { percentageOver -= 20f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver.coerceAtMost(20f) / 3.5f).also { percentageOver -= 20f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver.coerceAtMost(10f) / 4f).also { percentageOver -= 10f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver.coerceAtMost(10f) / 5f).also { percentageOver -= 10f }
        if (percentageOver <= 0) return deduction
        deduction += (percentageOver / 6f)
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
}