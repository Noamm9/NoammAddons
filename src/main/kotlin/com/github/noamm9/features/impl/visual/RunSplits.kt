package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonListener.DualTime
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import java.util.concurrent.ConcurrentHashMap

object RunSplits: Feature("A Splits HUD for Dungeons.") {
    private val floorSplits by lazy {
        DataDownloader.loadJson<Map<String, List<Map<String, String?>>>>("runSplits.json").mapValues {
            it.value.map { entryMap ->
                DialogueEntry(
                    entryMap["name"] ?: error("Missing 'name'"),
                    entryMap["start"],
                    entryMap["end"]
                )
            }
        }
    }

    private val runEndRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    private val currentFloorSplits = ConcurrentHashMap<String, Split>()

    private var currentText: List<String> = emptyList()
    private val exampleText = listOf(
        "§8Wither Doors: §7?",
        "§4Blood Open: ?",
        "§cWatcher Clear: ?",
        "§dPortal: ?",
        "§aBoss Entry: ?"
    )

    override fun init() {
        hudElement("Run Splits", shouldDraw = { LocationUtils.inDungeon }) { ctx, example ->
            val text = if (example) exampleText else currentText
            if (text.isEmpty()) return@hudElement 0f to 0f

            var currentY = 0f
            var width = 0f

            for (i in text.indices) {
                val line = text[i]
                Render2D.drawString(ctx, line, 0, currentY)
                width = maxOf(width, line.width().toFloat())
                currentY += 9f
            }

            return@hudElement width to currentY
        }

        register<WorldChangeEvent> { currentFloorSplits.clear() }

        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon) return@register

            val now = DualTime(DungeonListener.currentTime)
            val start = DungeonListener.dungeonStartTime ?: now
            val blood = DungeonListener.bloodOpenTime
            val watcher = DungeonListener.watcherClearTime
            val boss = DungeonListener.bossEntryTime

            val bloodOpen = when {
                blood == null && DungeonListener.dungeonStarted -> dual(now - start, ::formatTime)
                blood != null -> dual(blood - start, ::formatTime)
                else -> "?"
            }

            val watcherClear = when {
                watcher != null && blood != null -> dual(watcher - blood, ::formatSecs)
                blood != null -> dual(now - blood, ::formatSecs)
                else -> "?"
            }

            val portalTime = when {
                watcher != null && boss == null -> dual(now - watcher, ::formatDec)
                watcher != null && boss != null -> dual(boss - watcher, ::formatDec)
                else -> "?"
            }

            val bossEntry = if (DungeonListener.dungeonStarted) dual((boss ?: now) - start, ::formatTime) else "?"

            val splitLines = currentFloorSplits.mapNotNull { (name, split) ->
                val s = split.start ?: return@mapNotNull null
                val e = split.end ?: DualTime(DungeonListener.currentTime)
                val diff = e - s
                val t = (diff.ticks / 20.0).toFixed(2) + "s"
                val r = (diff.real / 1000.0).toFixed(2) + "s"
                "$name: ${r} §7(§b${t}§7)§r"
            }.toMutableList().apply {
                indexOfFirst { it.startsWith("&aBoss") }.takeUnless { it == - 1 }?.let { add(removeAt(it)) }
            }

            val clearInfo = listOf(
                "§8Wither Doors: §7${DungeonInfo.witherDoors}",
                "§4Blood Open: $bloodOpen",
                "§cWatcher Clear: $watcherClear",
                "§dPortal: $portalTime",
                "§aBoss Entry: $bossEntry"
            )

            currentText = if (splitLines.isEmpty()) clearInfo else clearInfo + "-----------------------" + splitLines
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.inDungeon) return@register

            val msg = event.unformattedText.trim()
            val floor = LocationUtils.dungeonFloor ?: return@register
            val currentSplits = floorSplits[floor] ?: floorSplits[floor.replace("M", "F")] ?: return@register

            currentSplits.forEachIndexed { i, entry ->
                val split = currentFloorSplits.getOrPut(entry.name) { Split() }

                if (entry.startMatches(msg) || (i > 0 && currentSplits[i - 1].endMatches(msg))) {
                    split.start = DualTime(DungeonListener.currentTime)
                }

                if (entry.endMatches(msg) || (entry.end == null && runEndRegex.matches(msg))) {
                    split.end = DualTime(DungeonListener.currentTime)
                }

                if (split.start != null || split.end != null) currentFloorSplits[entry.name] = split
            }
        }
    }

    private data class Split(var start: DualTime? = null, var end: DualTime? = null)
    private data class DialogueEntry(val name: String, val start: String? = null, val end: String? = null) {
        fun startMatches(msg: String) = start == msg || start?.toRegex()?.matches(msg) == true
        fun endMatches(msg: String) = end == msg || end?.toRegex()?.matches(msg) == true
    }

    private operator fun DualTime.minus(other: DualTime) = DualTime(ticks - other.ticks, real - other.real)
    private fun dual(diff: DualTime, fmt: (Long) -> String) = "${fmt(diff.real / 50)} §7(§b${fmt(diff.ticks)}§7)"
    private fun formatTime(ticks: Long) = "${(ticks / 20) / 60}m ${(ticks / 20) % 60}s"
    private fun formatSecs(ticks: Long) = "${ticks / 20}s"
    private fun formatDec(ticks: Long) = "${(ticks / 20.0).toFixed(1)}s"
}
