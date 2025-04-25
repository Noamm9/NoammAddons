package noammaddons.features.impl.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.hud.DungeonRunSplits.DungeonRunSplitsElement.overviewStr
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.ThreadUtils.loop
import java.awt.Color
import kotlin.math.roundToInt


object DungeonRunSplits: Feature() {
    private object DungeonRunSplitsElement: GuiElement(hudData.getData().dungeonRunSplits) {
        private val exampleText = listOf(
            "&8Wither Doors: &7?",
            "&4Blood Open: ?",
            "&cWatcher Clear: ?",
            "&dPortal: ?",
            "&aBoss Entry: ?"
        )

        var overviewStr = emptyList<String>()

        override val enabled get() = DungeonRunSplits.enabled
        override val width get() = RenderHelper.getStringWidth(currentText())
        override val height get() = RenderHelper.getStringHeight(currentText())

        private fun currentText(): List<String> {
            return if (HudEditorScreen.isOpen()) exampleText else overviewStr
        }

        override fun draw() {
            GlStateManager.pushMatrix()
            GlStateManager.scale(getScale(), getScale(), getScale())
            GlStateManager.translate(getX() / getScale(), getY() / getScale(), 0f)
            currentText().withIndex().forEach { (i, line) ->
                val y = i * 9f
                mc.fontRendererObj.drawStringWithShadow(line.addColor(), 0f, y, Color.WHITE.rgb)
            }
            GlStateManager.popMatrix()
        }
    }

    private data class DialogueEntry(val name: String, val start: String? = null, val end: String? = null)
    private class Split {
        var start: Long? = null
        var end: Long? = null
        var isPB = false
        var pbTime: Long? = null
        var pbTimeOld: Long? = null
    }

    private val floorSplits = mutableMapOf<String, List<DialogueEntry>>()

    private val currentFloorSplits = mutableMapOf<String, Split>()
    private val runEndRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")


    init {
        WebUtils.fetchJsonWithRetry<Map<String, List<Map<String, String?>>>>(
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
        onWorldLoad { currentFloorSplits.clear() }

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

            val threadSafeCopy = currentFloorSplits.toMap()
            if (threadSafeCopy.isNotEmpty()) {
                for ((name, split) in threadSafeCopy) {
                    val text = when {
                        split.start != null && split.end != null -> {
                            val duration = ((split.end !! - split.start !!) / 1000.0).toFixed(2).toDouble()
                            val pbTime = (split.pbTime !! / 1000.0).toFixed(2).toDouble()
                            val splitSuffix = if (split.isPB) {
                                val old = split.pbTimeOld?.let { (it / 1000.0).toFixed(2).toDouble() }
                                if (old != null) "&7(${old}s)" else ""
                            }
                            else "&e(${pbTime}s)"

                            "$name: ${duration}s&r $splitSuffix"
                        }

                        split.start != null && split.end == null -> {
                            val live = ((System.currentTimeMillis() - split.start !!) / 1000.0).toFixed(2)
                            "$name: ${live}s&r"
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
                else "-----------------------"
            )

            overviewStr = clearInfo + splitLines
        }

        onChat {
            if (! LocationUtils.inDungeon) return@onChat
            val msg = it.value
            val floor = LocationUtils.dungeonFloor ?: return@onChat
            val currentSplits = floorSplits[floor] ?: floorSplits[floor.replace("M", "F")] ?: return@onChat
            val pbData = personalBests.getData().dungeonSplits.getOrPut(floor) {
                mutableMapOf()
            }

            currentSplits.forEachIndexed { i, entry ->
                val split = currentFloorSplits.getOrPut(entry.name) { Split() }

                if (entry.startMatches(msg) || currentSplits.getOrNull(i - 1)?.endMatches(msg) == true) {
                    split.start = System.currentTimeMillis()
                }

                if (entry.endMatches(msg) || entry.end == null && runEndRegex.matches(msg)) {
                    split.end = System.currentTimeMillis()
                    val deltaTime = split.end !! - split.start !!
                    val pbTime = pbData[entry.name]
                    split.pbTime = pbTime
                    if (pbTime == null || pbTime > deltaTime) {
                        split.pbTimeOld = pbTime
                        split.pbTime = deltaTime
                        pbData[entry.name] = deltaTime
                        personalBests.save()
                        split.isPB = true
                    }
                }

                if (split.start != null || split.end != null) {
                    currentFloorSplits[entry.name] = split
                }
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
}