package com.github.noamm9.utils.dungeons

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.*
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.concurrent.*
import kotlin.math.abs

/**
 * Records the score/grade of every finished dungeon run and matches them to the runs listed in the Croesus menu,
 * so we know which unclaimed runs were S / S+ (S+ runs are the ones worth spending a Kismet Feather on).
 *
 * Croesus lists runs newest first, 28 per page, mixed with Kuudra runs, and drops a run 96 hours after it was completed.
 * Runs done without the mod can also be in there, so entries are aligned against our records by floor
 * instead of by raw position.
 */
object CroesusRunTracker: ISelfInit {
    const val EXPIRY_MS = 96 * 60 * 60 * 1000L
    private const val EXPIRY_GRACE_MS = 30 * 60 * 1000L
    private const val MAX_RUNS = 280
    private const val SCORE_TIMEOUT_MS = 5_000L

    private val teamScoreRegex = Regex("^\\s*Team Score: (?<score>\\d+) \\((?<grade>S\\+|S|A|B|C|D)\\).*$")
    private val profileRegex = Regex("^You are playing on profile: (?<profile>\\w+).*$")

    private val croesusMenuRegex = Regex("^(?:\\((?<page>\\d+)/\\d+\\) )?Croesus$")
    private val loreFloorRegex = Regex("^Floor (?<floor>[IVX]+)$")

    // Not confirmed in game. If Hypixel ever shows the grade or expiry timer in the run's lore, it is used over our records.
    private val loreScoreRegex = Regex("^(?:Team Score|Score|Grade|Rank): (?:(?<score>\\d+) )?\\(?(?<grade>S\\+|S|A|B|C|D)\\)?$")
    private val loreExpiryRegex = Regex("^(?:Chests? )?[Ee]xpires? [Ii]n:? (?<time>(?:\\d+[dhms] ?)+)$")
    private val durationPartRegex = Regex("(\\d+)([dhms])")

    data class TrackedRun(val floor: String, val score: Int, val grade: String, val time: Long, val estimated: Boolean = false)

    data class RunInfo(
        val floor: String?,
        val grade: String?,
        val score: Int?,
        val estimated: Boolean,
        val expiresAt: Long?,
        val kismetAvailable: Boolean,
        val chestsRemaining: Boolean
    ) {
        val isSPlus get() = grade == "S+"
        val worthReroll get() = isSPlus && kismetAvailable && chestsRemaining
    }

    private data class CroesusEntry(
        val slot: Int,
        val floor: String?,
        val loreGrade: String?,
        val loreScore: Int?,
        val completedAt: Long?,
        val timeTolerance: Long,
        val kismetAvailable: Boolean,
        val chestsRemaining: Boolean
    )

    private data class PendingRun(val floor: String, val time: Long, val scoreSnapshot: Int)

    private val lastProfile = PogObject("croesus_profile", "")
    private val storage = PogObject("croesus_runs", ConcurrentHashMap<String, List<TrackedRun>>())

    private var pendingRun: PendingRun? = null

    /** The run that just finished in the current world, used for the reward chests at the end of the run. */
    var latestRun: TrackedRun? = null
        private set

    /** The Croesus run the player clicked into, used for the chest menus opened from Croesus. */
    var selectedRun: RunInfo? = null
        private set

    private val seenPages = HashMap<Int, List<CroesusEntry>>()
    private var currentPageInfo = emptyMap<Int, RunInfo>()

    private val profileKey get() = lastProfile.get().ifBlank { "default" }

    override fun init() {
        register<ChatMessageEvent> {
            val text = event.unformattedText

            profileRegex.matchEntire(text)?.let {
                val profile = it.groups["profile"] !!.value.lowercase()
                if (profile != lastProfile.get()) lastProfile.set(profile)
                return@register
            }

            if (! LocationUtils.inDungeon) return@register

            DungeonListener.runEndRegex.matchEntire(text)?.let { match ->
                val floor = LocationUtils.dungeonFloor?.takeUnless { it.isBlank() } ?: floorFromRunEnd(match) ?: return@register
                pendingRun = PendingRun(floor, System.currentTimeMillis(), ScoreCalculation.score)
                return@register
            }

            teamScoreRegex.matchEntire(text)?.let { match ->
                val pending = pendingRun ?: PendingRun(LocationUtils.dungeonFloor ?: return@register, System.currentTimeMillis(), 0)
                val score = match.groups["score"] !!.value.toInt()
                val grade = match.groups["grade"] !!.value
                pendingRun = null
                record(TrackedRun(pending.floor, score, grade, pending.time))
            }
        }

        register<TickEvent.End> {
            val pending = pendingRun ?: return@register
            if (System.currentTimeMillis() - pending.time < SCORE_TIMEOUT_MS) return@register
            flushPendingAsEstimated()
        }

        register<WorldChangeEvent> {
            flushPendingAsEstimated()
            latestRun = null
            selectedRun = null
            seenPages.clear()
            currentPageInfo = emptyMap()
        }

        register<ContainerFullyOpenedEvent> {
            val match = croesusMenuRegex.matchEntire(event.title.unformattedText)
            if (match == null) {
                currentPageInfo = emptyMap()
                return@register
            }

            val page = match.groups["page"]?.value?.toIntOrNull() ?: 1
            if (page == 1) seenPages.clear()

            seenPages[page] = contentSlots.mapNotNull { slot -> event.items[slot]?.let { parseEntry(slot, it) } }
            seenPages[page]?.firstOrNull()?.let { first ->
                ChatUtils.debug("croesus", "§7First run lore: §r${event.items[first.slot]?.lore?.joinToString("§7 | §r")}")
            }
            currentPageInfo = resolvePage(page)
        }

        register<ContainerEvent.SlotClick> {
            if (croesusMenuRegex.matchEntire(event.screen.title.unformattedText) == null) return@register
            if (event.slotId !in contentSlots) return@register
            selectedRun = currentPageInfo[event.slotId]
        }
    }

    /** Info for a run slot in the currently open Croesus page, null when it isn't a dungeon run or it's unknown. */
    fun getCroesusRun(slot: Int) = currentPageInfo[slot]

    /** The run whose reward chests are currently being looked at, either at the end of the run or through Croesus. */
    fun getActiveChestRun(): RunInfo? = when (LocationUtils.world) {
        WorldType.Catacombs -> latestRun?.let { RunInfo(it.floor, it.grade, it.score, it.estimated, it.time + EXPIRY_MS, true, true) }
        WorldType.DungeonHub -> selectedRun
        else -> null
    }

    fun isCroesusMenu(title: String) = croesusMenuRegex.matches(title)

    fun gradeFromScore(score: Int) = when {
        score >= 300 -> "S+"
        score >= 270 -> "S"
        score >= 230 -> "A"
        score >= 160 -> "B"
        score >= 100 -> "C"
        else -> "D"
    }

    private fun flushPendingAsEstimated() {
        val pending = pendingRun ?: return
        pendingRun = null
        if (pending.scoreSnapshot <= 0) return
        record(TrackedRun(pending.floor, pending.scoreSnapshot, gradeFromScore(pending.scoreSnapshot), pending.time, estimated = true))
    }

    private fun record(run: TrackedRun) {
        // Entrance runs don't go to Croesus.
        if (run.floor == "E") return

        val runs = activeRuns()
        if (runs.firstOrNull()?.let { it.floor == run.floor && abs(it.time - run.time) < 60_000 } == true) return

        storage.get()[profileKey] = (listOf(run) + runs).take(MAX_RUNS)
        storage.save()
        latestRun = run

        val estimated = if (run.estimated) " §7(estimated)" else ""
        ChatUtils.debug("croesus", "§7Recorded §e${run.floor} §7run: ${gradeColor(run.grade)}${run.grade} §8(${run.score})$estimated")
    }

    /** Runs Croesus may still be holding, newest first. Expired runs are pruned from storage. */
    private fun activeRuns(): List<TrackedRun> {
        val now = System.currentTimeMillis()
        val runs = storage.get()[profileKey] ?: return emptyList()
        // A short grace period covers small differences between our timestamp and Hypixel's.
        // Leftover runs Croesus already dropped simply don't get aligned to anything.
        val active = runs.filter { now - it.time < EXPIRY_MS + EXPIRY_GRACE_MS }.sortedByDescending { it.time }.take(MAX_RUNS)
        if (active.size != runs.size) {
            storage.get()[profileKey] = active
            storage.save()
        }
        return active
    }

    private fun parseEntry(slot: Int, stack: ItemStack): CroesusEntry? {
        if (stack.item != Items.PLAYER_HEAD) return null
        val name = stack.hoverName.unformattedText
        if (name != "The Catacombs" && name != "Master Mode The Catacombs") return null

        val lore = stack.lore
        val cleanLore = lore.map { it.removeFormatting().trim() }

        val floorNumber = cleanLore.firstNotNullOfOrNull { loreFloorRegex.matchEntire(it)?.groups?.get("floor")?.value?.romanToDecimal() }
        val floor = floorNumber?.let { (if (name.startsWith("Master Mode")) "M" else "F") + it }

        val scoreMatch = cleanLore.firstNotNullOfOrNull { loreScoreRegex.matchEntire(it) }
        val loreScore = scoreMatch?.groups?.get("score")?.value?.toIntOrNull()
        val loreGrade = scoreMatch?.groups?.get("grade")?.value

        var completedAt: Long? = null
        var tolerance = 0L
        cleanLore.firstNotNullOfOrNull { loreExpiryRegex.matchEntire(it)?.groups?.get("time")?.value }?.let { time ->
            val parts = durationPartRegex.findAll(time).map { it.groupValues[1].toLong() to unitMs(it.groupValues[2]) }.toList()
            if (parts.isEmpty()) return@let
            completedAt = System.currentTimeMillis() + parts.sumOf { (amount, unit) -> amount * unit } - EXPIRY_MS
            tolerance = parts.minOf { it.second } + 2 * 60 * 1000L
        }

        return CroesusEntry(
            slot = slot,
            floor = floor,
            loreGrade = loreGrade,
            loreScore = loreScore,
            completedAt = completedAt,
            timeTolerance = tolerance,
            kismetAvailable = lore.any { it.removeFormatting().trim() == "Kismet Feather" && "§m" !in it },
            chestsRemaining = cleanLore.none { it == "No more chests to open!" }
        )
    }

    private fun resolvePage(page: Int): Map<Int, RunInfo> {
        val pageEntries = seenPages[page] ?: return emptyMap()
        if (pageEntries.isEmpty()) return emptyMap()

        // Croesus can only be paged through in order, so the earlier pages should have been seen already.
        // If they weren't, the position of this page is unknown and only lore timestamps can be trusted.
        val previousPages = (1 until page).map { seenPages[it] }
        val positionKnown = previousPages.all { it != null }
        val sequence = if (positionKnown) previousPages.flatMap { it !! } + pageEntries else pageEntries
        val pageStart = sequence.size - pageEntries.size

        val matches = alignNewestFirst(sequence, activeRuns()) { entry, run -> isCompatible(entry, run, positionKnown) }

        val result = HashMap<Int, RunInfo>()
        pageEntries.forEachIndexed { i, entry ->
            val run = matches[pageStart + i]
            val grade = entry.loreGrade ?: run?.grade
            result[entry.slot] = RunInfo(
                floor = entry.floor ?: run?.floor,
                grade = grade,
                score = entry.loreScore ?: run?.score,
                estimated = entry.loreGrade == null && run?.estimated == true,
                expiresAt = entry.completedAt?.plus(EXPIRY_MS) ?: run?.time?.plus(EXPIRY_MS),
                kismetAvailable = entry.kismetAvailable,
                chestsRemaining = entry.chestsRemaining
            )
        }

        ChatUtils.debug("croesus", "§7Page $page: matched §e${pageEntries.indices.count { matches[pageStart + it] != null }}§7/${pageEntries.size} runs §8(position known: $positionKnown)")
        return result
    }

    private fun isCompatible(entry: CroesusEntry, run: TrackedRun, positionKnown: Boolean): Boolean {
        if (entry.floor != null && entry.floor != run.floor) return false
        if (entry.loreGrade != null && ! run.estimated && entry.loreGrade != run.grade) return false
        entry.completedAt?.let { return abs(it - run.time) <= entry.timeTolerance }
        return positionKnown
    }

    private fun floorFromRunEnd(match: MatchResult): String? {
        if (match.groupValues[2] == "Entrance") return "E"
        val number = match.groupValues[3].romanToDecimal().takeIf { it > 0 } ?: return null
        return (if (match.groupValues[1].isNotEmpty()) "M" else "F") + number
    }

    private fun unitMs(unit: String) = when (unit) {
        "d" -> 24 * 60 * 60 * 1000L
        "h" -> 60 * 60 * 1000L
        "m" -> 60 * 1000L
        else -> 1000L
    }

    fun gradeColor(grade: String?) = when (grade) {
        "S+" -> "§6"
        "S" -> "§e"
        "A" -> "§a"
        null -> "§8"
        else -> "§c"
    }

    private val contentSlots = (1 .. 4).flatMap { row -> (row * 9 + 1) .. (row * 9 + 7) }
}

/**
 * Aligns two newest-first run lists as a longest common subsequence, so runs missing on either side
 * (played without the mod, already expired, Kuudra...) don't shift everything after them.
 * Among equally good alignments it prefers matching as early as possible, which is plain positional matching when nothing is missing.
 *
 * @return index in [entries] to the run it was matched with
 */
internal fun <E, R> alignNewestFirst(entries: List<E>, runs: List<R>, compatible: (E, R) -> Boolean): Map<Int, R> {
    val n = entries.size
    val m = runs.size
    val canMatch = Array(n) { i -> BooleanArray(m) { j -> compatible(entries[i], runs[j]) } }
    val dp = Array(n + 1) { IntArray(m + 1) }

    for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
        val match = if (canMatch[i][j]) dp[i + 1][j + 1] + 1 else 0
        dp[i][j] = maxOf(match, dp[i + 1][j], dp[i][j + 1])
    }

    val result = HashMap<Int, R>()
    var i = 0
    var j = 0
    while (i < n && j < m) when {
        canMatch[i][j] && dp[i][j] == dp[i + 1][j + 1] + 1 -> result[i ++] = runs[j ++]
        dp[i + 1][j] >= dp[i][j + 1] -> i ++
        else -> j ++
    }

    return result
}
