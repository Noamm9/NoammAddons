package noammaddons.features.impl.dungeons.dmap.handlers

import gg.essential.elementa.state.BasicState
import noammaddons.features.impl.alerts.CryptsDone
import noammaddons.features.impl.dungeons.MimicDetector
import noammaddons.utils.DungeonUtils
import noammaddons.utils.DungeonUtils.watcherClearTime
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.ScoreboardUtils
import kotlin.math.floor


object ScoreCalculation {
    private val deathsPattern = Regex("§r§a§l(?:Team )?Deaths: §r§f\\((?<deaths>\\d+)\\)§r")
    private val puzzlePattern = Regex("§r (?<puzzle>.+): §r§7\\[§r§[ac6]§l(?<state>[✔✖✦])§r§7].+")
    private val puzzleCountRegex = Regex("§r§b§lPuzzles: §r§f\\((?<count>\\d)\\)§r")
    private val secretsFoundPattern = Regex("§r Secrets Found: §r§b(?<secrets>\\d+)§r")
    private val secretsFoundPercentagePattern = Regex("§r Secrets Found: §r§[ae](?<percentage>[\\d.]+)%§r")
    private val cryptsPattern = Regex("§r Crypts: §r§6(?<crypts>\\d+)§r")
    private val completedRoomsRegex = Regex("§r Completed Rooms: §r§d(?<count>\\d+)§r")
    private val dungeonClearedPattern = Regex("Cleared: (?<percentage>\\d+)% \\(\\d+\\)")
    private val timeElapsedPattern = Regex(" Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")

    var dungeonStats = DungeonStats().apply { cryptsCount.onSetValue(CryptsDone.func) }
    private var puzzles = mutableListOf<Pair<String, Boolean?>>()

    data class DungeonStats(
        var deathCount: Int = 0,
        var secretCount: Int = 0,
        var cryptsCount: BasicState<Int> = BasicState(0),
        var secretPercentage: Double = 0.0,
        var clearedPercentage: Int = 0,
        var completedRooms: Int = 0,
        var puzzleCount: Int = 0,
        var secondsElapsed: Int = 0,
        var mimicKilled: Boolean = false,
        var princeKilled: Boolean = false,
        var bloodDone: Boolean = false
    )

    private val totalRooms
        get() = if (dungeonStats.completedRooms > 0 && dungeonStats.clearedPercentage > 0)
            floor((dungeonStats.completedRooms / (dungeonStats.clearedPercentage / 100.0)) + 0.4).toInt()
        else 36

    val score: Int
        get() {
            val currentFloor = dungeonFloor ?: return 0
            val effectiveCompletedRooms = dungeonStats.completedRooms + (if (watcherClearTime == null) 1 else 0) + (if (! inBoss) 1 else 0)

            val secretRatio = (dungeonStats.secretPercentage / (requiredSecretPercentage[currentFloor] ?: 100.0)).coerceIn(0.0, 1.0)
            val exploreFromSecrets = (secretRatio * 40).toInt()
            val exploreFromRooms = (effectiveCompletedRooms.toFloat() / totalRooms * 60f).coerceIn(0f, 60f).toInt()
            val explorationScore = exploreFromSecrets + exploreFromRooms

            val skillFromRooms = (effectiveCompletedRooms.toFloat() / totalRooms * 80f).coerceIn(0f, 80f).toInt()
            val puzzlePenalty = (dungeonStats.puzzleCount - puzzles.count { it.second == true }) * 10
            val deathPenalty = (dungeonStats.deathCount * 2 - 1).coerceAtLeast(0) // assumes Spirit pet
            val skillScore = (20 + skillFromRooms - puzzlePenalty - deathPenalty).coerceAtLeast(0)

            return explorationScore + skillScore + bonusScore + speedScore
        }

    val bonusScore: Int
        get() {
            var score = dungeonStats.cryptsCount.get().coerceAtMost(5)
            if (MimicDetector.mimicKilled.get()) score += 2
            if (MimicDetector.princeKilled.get()) score += 1
            if (DungeonUtils.isPaul()) score += 10
            return score
        }

    val speedScore: Int
        get() {
            val limit = timeLimit[dungeonFloor] ?: return 100
            if (dungeonStats.secondsElapsed <= limit) return 100
            val percentageOver = (dungeonStats.secondsElapsed - limit) * 100f / limit
            return (100 - getSpeedDeduction(percentageOver)).toInt().coerceAtLeast(0)
        }

    fun onWorldUnload() {
        dungeonStats = DungeonStats().apply { cryptsCount.onSetValue(CryptsDone.func) }
        puzzles.clear()
    }

    fun updateFromTab(tabEntries: List<String>) {
        var readingPuzzles = false
        puzzles.clear()

        tabEntries.forEach { line ->
            when {
                readingPuzzles -> puzzlePattern.find(line)?.let { matcher ->
                    matcher.groups["puzzle"]?.value?.let { name ->
                        val state = when (matcher.groups["state"]?.value) {
                            "✔" -> true
                            "✖" -> false
                            else -> null
                        }
                        if (! name.contains("???")) puzzles.add(Pair(name, state))
                    }
                } ?: run { readingPuzzles = false }

                line.contains("Deaths:") -> deathsPattern.find(line)?.let { dungeonStats.deathCount = it.groups["deaths"]?.value?.toIntOrNull() ?: dungeonStats.deathCount }

                line.contains("Secrets Found:") -> if (line.contains('%')) secretsFoundPercentagePattern.find(line)
                    ?.let { dungeonStats.secretPercentage = it.groups["percentage"]?.value?.toDoubleOrNull() ?: dungeonStats.secretPercentage }
                else secretsFoundPattern.find(line)?.let { dungeonStats.secretCount = it.groups["secrets"]?.value?.toIntOrNull() ?: dungeonStats.secretCount }

                line.contains("Crypts:") -> cryptsPattern.find(line)?.let { dungeonStats.cryptsCount.set(it.groups["crypts"]?.value?.toIntOrNull() ?: dungeonStats.cryptsCount.get()) }
                line.contains("Completed Rooms:") -> completedRoomsRegex.find(line)?.let { dungeonStats.completedRooms = it.groups["count"]?.value?.toIntOrNull() ?: dungeonStats.completedRooms }
                line.contains("Puzzles:") -> {
                    readingPuzzles = true
                    puzzleCountRegex.find(line)?.let { dungeonStats.puzzleCount = it.groups["count"]?.value?.toIntOrNull() ?: dungeonStats.puzzleCount }
                }
            }
        }
    }

    fun updateFromScoreboard() {
        ScoreboardUtils.sidebarLines.forEach { line ->
            val str = ScoreboardUtils.cleanSB(line)
            when {
                str.startsWith("Cleared:") -> dungeonClearedPattern.find(str)
                    ?.let { dungeonStats.clearedPercentage = it.groups["percentage"]?.value?.toIntOrNull() ?: dungeonStats.clearedPercentage }

                str.startsWith("Time Elapsed:") -> timeElapsedPattern.find(str)?.let { matcher ->
                    val hours = matcher.groups["hrs"]?.value?.toIntOrNull() ?: 0
                    val minutes = matcher.groups["min"]?.value?.toIntOrNull() ?: 0
                    val seconds = matcher.groups["sec"]?.value?.toIntOrNull() ?: 0
                    dungeonStats.secondsElapsed = (hours * 3600 + minutes * 60 + seconds)
                }
            }
        }
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
        "E" to 30.0, "F1" to 30.0, "F2" to 40.0, "F3" to 50.0, "F4" to 60.0,
        "F5" to 70.0, "F6" to 85.0, "F7" to 100.0, "M1" to 100.0, "M2" to 100.0,
        "M3" to 100.0, "M4" to 100.0, "M5" to 100.0, "M6" to 100.0, "M7" to 100.0
    )

    private val timeLimit = mapOf(
        "E" to 600, "F1" to 600, "F2" to 600, "F3" to 600, "F4" to 720,
        "F5" to 600, "F6" to 720, "F7" to 840, "M1" to 480, "M2" to 480,
        "M3" to 480, "M4" to 480, "M5" to 480, "M6" to 600, "M7" to 840
    )
}