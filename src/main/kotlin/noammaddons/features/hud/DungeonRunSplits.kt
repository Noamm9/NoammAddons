package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.hud.DungeonRunSplits.DungeonRunSplitsElement.overviewStr
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.ThreadUtils.loop
import kotlin.math.roundToInt

//
object DungeonRunSplits: Feature() {
    private object DungeonRunSplitsElement: GuiElement(hudData.getData().dungeonRunSplits) {
        private val exampleText = listOf(
            "&8Wither Doors: &7?",
            "&4Blood Open: ?",
            "&cWatcher Clear: ?",
            "&dPortal: ?",
            "&aBoss Entry: ?"
        )
        var overviewStr: String = exampleText.joinToString("\n")

        override val enabled get() = config.dungeonRunSplits
        override val width get() = RenderHelper.getStringWidth(overviewStr)
        override val height get() = RenderHelper.getStringHeight(overviewStr)
        override fun draw() = RenderUtils.drawText(overviewStr, getX(), getY(), getScale())
        override fun exampleDraw() = RenderUtils.drawText(exampleText, getX(), getY(), getScale())
    }

    private data class DialogueEntry(val name: String, val start: String? = null, val end: String? = null)
    private data class Split(var start: Long? = null, var end: Long? = null)

    private val floorSplits = mutableMapOf<String, List<DialogueEntry>>()

    private val currentFloorSplits = mutableMapOf<String, Split>()
    private val runEndRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")


    init {
        JsonUtils.fetchJsonWithRetry<Map<String, List<Map<String, String?>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/runSplits.json"
        ) {
            it ?: return@fetchJsonWithRetry

            val data = it.mapValues { (_, entryList) ->
                entryList.map { entryMap ->
                    DialogueEntry(
                        name = entryMap["name"] ?: error("Missing 'name' in DialogueEntry"),
                        start = entryMap["start"],
                        end = entryMap["end"]
                    )
                }
            }

            floorSplits.clear()
            floorSplits.putAll(data)
        }
    }

    private fun formatTime(ms: Long): String = "${(ms / 1000) / 60}m ${(ms / 1000) % 60}s"
    private fun formatSecs(ms: Long): String = "${ms / 1000}s"
    private fun formatMillisAsDecimal(ms: Long): String = "${(ms / 10.0).roundToInt() / 100.0}s"
    private fun DialogueEntry.startMatches(msg: String) = start == msg || start?.toRegex()?.matches(msg) == true
    private fun DialogueEntry.endMatches(msg: String) = end == msg || end?.toRegex()?.matches(msg) == true


    init {
        loop(50) {
            if (! LocationUtils.inDungeon) return@loop

            val splitLines = mutableListOf<String>()

            val bloodOpen = when {
                DungeonUtils.bloodOpenTime == null && DungeonUtils.dungeonStarted ->
                    formatTime(System.currentTimeMillis() - (DungeonUtils.dungeonStartTime ?: return@loop))

                DungeonUtils.bloodOpenTime != null ->
                    formatTime(DungeonUtils.bloodOpenTime !! - (DungeonUtils.dungeonStartTime ?: return@loop))

                else -> "?"
            }

            val watcherClear = when {
                DungeonUtils.watcherClearTime != null ->
                    formatSecs(DungeonUtils.watcherClearTime !! - (DungeonUtils.bloodOpenTime ?: return@loop))

                DungeonUtils.bloodOpenTime != null ->
                    formatSecs(System.currentTimeMillis() - DungeonUtils.bloodOpenTime !!)

                else -> "?"
            }

            val portalTime = when {
                DungeonUtils.watcherClearTime != null && DungeonUtils.bossEntryTime == null ->
                    formatMillisAsDecimal(System.currentTimeMillis() - DungeonUtils.watcherClearTime !!)

                DungeonUtils.watcherClearTime != null && DungeonUtils.bossEntryTime != null ->
                    formatMillisAsDecimal(DungeonUtils.bossEntryTime !! - DungeonUtils.watcherClearTime !!)

                else -> "?"
            }

            val bossEntry = if (DungeonUtils.dungeonStarted)
                formatTime((DungeonUtils.bossEntryTime ?: System.currentTimeMillis()) - (DungeonUtils.dungeonStartTime ?: return@loop))
            else "?"

            if (currentFloorSplits.isNotEmpty()) {
                for ((name, split) in currentFloorSplits) {
                    val text = when {
                        split.start != null && split.end != null -> {
                            val duration = ((split.end !! - split.start !!) / 1000.0).toFixed(2)
                            "$name: ${duration}s&r"
                        }

                        split.start != null && split.end == null -> {
                            val live = ((System.currentTimeMillis() - split.start !!) / 1000.0).toFixed(2)
                            "$name: ${live}s"
                        }

                        else -> continue
                    }
                    splitLines.add(text)
                }
            }

            val clearInfo = listOf(
                "§8Wither Doors: §7${DungeonInfo.witherDoors}",
                "§4Blood Open: $bloodOpen",
                "§cWatcher Clear: $watcherClear",
                "§dPortal: $portalTime",
                "§aBoss Entry: $bossEntry",
                if (splitLines.isEmpty()) ""
                else "--------------------"
            )

            overviewStr = (clearInfo + splitLines).joinToString("\n")
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! LocationUtils.inDungeon) return
        val msg = event.component.noFormatText
        val floor = LocationUtils.dungeonFloor ?: return
        val currentSplits = floorSplits[floor] ?: floorSplits[floor.replace("M", "F")] ?: return

        currentSplits.forEachIndexed { i, entry ->
            val split = currentFloorSplits.getOrPut(entry.name) { Split() }

            if (entry.startMatches(msg) || currentSplits.getOrNull(i - 1)?.endMatches(msg) == true) {
                split.start = System.currentTimeMillis()
            }
            if (entry.endMatches(msg) || entry.end == null && runEndRegex.matches(msg)) {
                split.end = System.currentTimeMillis()
            }

            if (split.start != null || split.end != null) {
                currentFloorSplits[entry.name] = split
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! DungeonRunSplitsElement.enabled) return
        if (mc.currentScreen is HudEditorScreen) return
        if (! LocationUtils.inDungeon) return
        DungeonRunSplitsElement.draw()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = currentFloorSplits.clear()
}