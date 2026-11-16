package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonListener.DualTime
import com.github.noamm9.utils.dungeons.DungeonListener.DualTime.Companion.minus
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString

object RunSplits: Feature("A Splits HUD for Dungeons.") {
    private val showWitherDoors by ToggleSetting("Show Wither Doors").withDescription("Show The Number of Wither Doors in the run")
    private val showTotalTime by ToggleSetting("Total Time").withDescription("Shows the total run time.")
    private val showTimeLost by ToggleSetting("Show Time Lost").withDescription("Shows the total time run time that was lost due to server lags.")
    private val show300Score by ToggleSetting("Show 300 Score Time").withDescription("Shows when 300 score was reached.")
    private val printSplits by ToggleSetting("Print Splits").withDescription("Prints the splits into chat at the end of the run.")

    private val runEndRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    private const val separator = "-----------------------"

    private val splitTimes = mutableListOf<DualTime>()

    //#if LEGIT private var witherDoors = 0

    private var score300Timer: DualTime? = null

    private var currentText: List<String> = emptyList()
    private val exampleText = listOf(
        "§8Wither Doors: §72",
        "§4Blood Open: 1:07 §7(§b1:06§7)",
        "§cWatcher Clear: 67 §7(§b66§7)",
        "§dPortal: 4.2 §7(§b4.1§7)",
        "§e300 Score: 1:22 §7(§b1:22§7)",
        "§aBoss Entry: 5:49 §7(§b5:47§7)"
    )

    override fun init() {
        hudElement("Run Splits", shouldDraw = { LocationUtils.inDungeon }) { ctx, example ->
            val text = if (example) exampleText else currentText
            if (text.isEmpty()) return@hudElement 0f to 0f

            var currentY = 0
            var width = 0

            for (i in text.indices) {
                val line = text[i]
                ctx.drawString(line, 0, currentY)
                width = maxOf(width, line.length * 4)
                currentY += 9
            }

            return@hudElement width to currentY
        }

        register<WorldChangeEvent> {
            splitTimes.clear()
            score300Timer = null
            currentText = emptyList()
            //#if LEGIT witherDoors = 0
        }

        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon) return@register
            if (DungeonListener.dungeonEnded) return@register

            updateText()
        }

        register<DungeonEvent.Score> {
            if (score300Timer != null) return@register
            if (event.score < 300) return@register
            score300Timer = DualTime(DungeonListener.currentTime)
        }

        register<DungeonEvent.NecronDeathEvent> {
            val splits = currentSplits() ?: return@register
            if (splitTimes.size !in 1 until splits.splits.size) return@register
            splitTimes.add(DualTime(DungeonListener.currentTime))
        }

        register<DungeonEvent.RunEndedEvent> {
            if (! printSplits.value) return@register
            ThreadUtils.scheduledTask(2) {
                updateText()
                ChatUtils.modMessage("&bRun Splits:")
                currentText.forEach(ChatUtils::chat)
            }
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.inDungeon) return@register
            val msg = event.unformattedText.trim()
            //#if LEGIT if (msg.matches(DungeonListener.witherDoorOpenedRegex)) witherDoors ++
            val splits = currentSplits() ?: return@register
            val reached = when {
                splitTimes.isEmpty() -> msg == splits.start
                splitTimes.size > splits.splits.size -> false
                msg.matches(runEndRegex) -> true
                else -> splitTimes.size < splits.splits.size && splits.splits[splitTimes.size - 1].ends(msg)
            }

            if (reached) splitTimes.add(DualTime(DungeonListener.currentTime))
        }
    }

    private fun updateText() {
        val now = DualTime(DungeonListener.dungeonEndTime ?: DungeonListener.currentTime)
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

        val score300 = when {
            ! show300Score.value -> null
            score300Timer != null -> dual(score300Timer !! - start, ::formatTime)
            DungeonListener.dungeonStarted -> dual(now - start, ::formatTime)
            else -> "?"
        }

        val portalTime = when {
            watcher != null && boss == null -> dual(now - watcher, ::formatDec)
            watcher != null && boss != null -> dual(boss - watcher, ::formatDec)
            else -> "?"
        }

        val bossEntry = if (DungeonListener.dungeonStarted) dual((boss ?: now) - start, ::formatTime) else "?"

        val splitLines = currentSplits()?.takeIf { splitTimes.isNotEmpty() }?.let { splits ->
            val live = DualTime(DungeonListener.currentTime)
            val lines = splits.splits.take(splitTimes.size).mapIndexed { i, split ->
                splitLine(split.name, splitTimes[i], splitTimes.getOrElse(i + 1) { live })
            }
            val bossEnd = if (splitTimes.size > splits.splits.size) splitTimes.last() else live
            lines + splitLine("&aBoss", splitTimes.first(), bossEnd)
        }.orEmpty()

        val clearInfo = mutableListOf<String>()
        if (showWitherDoors.value) {
            //#if LEGIT
            //!clearInfo.add("§8Wither Doors: §7$witherDoors")
            //#else
            clearInfo.add("§8Wither Doors: §7${DungeonScanner.witherDoors}")
            //#endif
        }
        clearInfo.add("§4Blood Open: $bloodOpen")
        clearInfo.add("§cWatcher Clear: $watcherClear")
        clearInfo.add("§dPortal: $portalTime")
        score300?.let { clearInfo.add("§e300 Score: $it") }
        clearInfo.add("§aBoss Entry: $bossEntry")

        if (splitLines.isNotEmpty()) {
            clearInfo.add(separator)
            clearInfo.addAll(splitLines)
        }

        if (DungeonListener.dungeonStarted) {
            val total = now - start
            val diffMs = total.real - (total.ticks * 50)

            if (showTotalTime.value || showTimeLost.value) clearInfo.add(separator)
            if (showTotalTime.value) clearInfo.add("§eTotal: ${dual(total, ::formatTime)}")
            if (showTimeLost.value && diffMs >= 1000) clearInfo.add("§8Lag Lost: §c+${diffMs / 1000}s")
        }

        currentText = clearInfo
    }

    private fun currentSplits(): BossSplits? {
        val floor = LocationUtils.dungeonFloor ?: return null
        return floorSplits[floor] ?: floorSplits[floor.replace("M", "F")]
    }

    private fun splitLine(name: String, start: DualTime, end: DualTime): String {
        val diff = end - start
        return "$name: ${(diff.real / 1000.0).toFixed(2)} §7(§b${(diff.ticks / 20.0).toFixed(2)}§7)§r"
    }

    private class Split(val name: String, val ends: (String) -> Boolean) {
        constructor(name: String, end: String? = null): this(name, { it == end })
    }

    private class BossSplits(val start: String, val splits: List<Split>) {
        constructor(start: String, vararg splits: Split): this(start, splits.toList())
    }

    private fun dual(diff: DualTime, fmt: (Long) -> String) = "${fmt(diff.real / 50)} §7(§b${fmt(diff.ticks)}§7)"
    private fun formatTime(ticks: Long) = "${ticks / 20 / 60}:${(ticks / 20 % 60).toString().padStart(2, '0')}"
    private fun formatSecs(ticks: Long) = "${ticks / 20}"
    private fun formatDec(ticks: Long) = if (ticks / 20 >= 60) formatTime(ticks) else (ticks / 20.0).toFixed(1)

    //@formatter:off
    private val floorSplits = buildMap {
        put("F1", BossSplits(
            "[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable.",
            Split("&cFirst Kill", "[BOSS] Bonzo: Oh noes, you got me.. what ever will I do?!"),
            Split("&cSecond Kill", "[BOSS] Bonzo: Alright, maybe I'm just weak after all.."),
            Split("&eEnd Dialogue")
        ))
        put("F2", BossSplits(
            "[BOSS] Scarf: This is where the journey ends for you, Adventurers.",
            Split("&cUndeads", "[BOSS] Scarf: Those toys are not strong enough I see."),
            Split("&bScarf")
        ))
        put("F3", BossSplits(
            "[BOSS] The Professor: I was burdened with terrible news recently...",
            Split("&9Guardians", "[BOSS] The Professor: Oh? You found my Guardians' one weakness?"),
            Split("&eThe Professor", "[BOSS] The Professor: I see. You have forced me to use my ultimate technique."),
            Split("&bGuardifessor")
        ))
        put("F4", BossSplits(
            "[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!",
            Split("&eDialogue", "[BOSS] Thorn: Dance! Dance with my Spirit animals! And may you perish in a delightful way!"),
            Split("&bThorn Kill")
        ))
        put("F5", BossSplits(
            "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.",
            Split("&eStart Dialogue", "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing."),
            Split("&cLivid Kill", Regex("\\[BOSS] \\w+ Livid: Impossible! How did you figure out which one I was\\?!")::matches),
            Split("&eEnd Dialogue")
        ))
        put("F6", BossSplits(
            "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!",
            Split("&6Terracottas", "[BOSS] Sadan: ENOUGH!"),
            Split("&dGiants", "[BOSS] Sadan: You did it. I understand now, you have earned my respect."),
            Split("&cSadan")
        ))

        val f7Splits = BossSplits(
            "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!",
            Split("&5Maxor", "[BOSS] Storm: Pathetic Maxor, just like expected."),
            Split("&bStorm", "[BOSS] Goldor: Who dares trespass into my domain?"),
            Split("&eTerminals", "The Core entrance is opening!"),
            Split("&7Goldor", "[BOSS] Necron: You went further than any human before, congratulations."),
            Split("&cNecron")
        )

        put("F7", f7Splits)
        put("M7", BossSplits(f7Splits.start, f7Splits.splits + Split("&4Dragons")))
    }
}