package noammaddons.features.impl.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.config.editgui.GuiElement
import noammaddons.config.editgui.HudEditorScreen
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.hud.RunSplits.DungeonRunSplitsElement.overviewStr
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.ThreadUtils.loop
import java.awt.Color
import kotlin.math.roundToInt

@AlwaysActive
object RunSplits: Feature() {
    private object DungeonRunSplitsElement: GuiElement(hudData.getData().dungeonRunSplits) {
        private val exampleText = listOf(
            "&8Wither Doors: &7?",
            "&4Blood Open: ?",
            "&cWatcher Clear: ?",
            "&dPortal: ?",
            "&aBoss Entry: ?"
        )

        var overviewStr = emptyList<String>()

        override val enabled get() = RunSplits.enabled
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
    private data class Split(var start: Long? = null, var end: Long? = null, var isPB: Boolean = false, var pbTime: Long? = null, var pbTimeOld: Long? = null)

    private val floorSplits = DataDownloader.loadJson<Map<String, List<Map<String, String?>>>>("runSplits.json").mapValues {
        it.value.map { entryMap ->
            DialogueEntry(entryMap["name"] ?: error("Missing 'name' in DialogueEntry"), entryMap["start"], entryMap["end"])
        }
    }

    private val runEndRegex = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")
    private val currentFloorSplits = mutableMapOf<String, Split>()
    var currentTime = 0L

    private fun formatTime(ms: Long): String = "${(ms / 1000) / 60}m ${(ms / 1000) % 60}s"
    private fun formatSecs(ms: Long): String = "${ms / 1000}s"
    private fun formatMillisAsDecimal(ms: Long): String = "${((ms / 10.0).roundToInt() / 100.0).toFixed(1)}s"
    private fun DialogueEntry.startMatches(msg: String) = start == msg || start?.toRegex()?.matches(msg) == true
    private fun DialogueEntry.endMatches(msg: String) = end == msg || end?.toRegex()?.matches(msg) == true

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        currentFloorSplits.clear()
        currentTime = 0
    }

    override fun init() = loop(50) {
        if (! enabled) return@loop
        if (! LocationUtils.inDungeon) return@loop

        val splitLines = mutableListOf<String>()

        val bloodOpen = when {
            DungeonUtils.bloodOpenTime == null && DungeonUtils.dungeonStarted ->
                formatTime(currentTime - DungeonUtils.dungeonStartTime !!)

            DungeonUtils.bloodOpenTime != null ->
                formatTime(DungeonUtils.bloodOpenTime !! - DungeonUtils.dungeonStartTime !!)

            else -> "?"
        }

        val watcherClear = when {
            DungeonUtils.watcherClearTime != null ->
                formatSecs(DungeonUtils.watcherClearTime !! - DungeonUtils.bloodOpenTime !!)

            DungeonUtils.bloodOpenTime != null ->
                formatSecs(currentTime - DungeonUtils.bloodOpenTime !!)

            else -> "?"
        }

        val portalTime = when {
            DungeonUtils.watcherClearTime != null && DungeonUtils.bossEntryTime == null ->
                formatMillisAsDecimal(currentTime - DungeonUtils.watcherClearTime !!)

            DungeonUtils.watcherClearTime != null && DungeonUtils.bossEntryTime != null ->
                formatMillisAsDecimal(DungeonUtils.bossEntryTime !! - DungeonUtils.watcherClearTime !!)

            else -> "?"
        }

        val bossEntry = if (DungeonUtils.dungeonStarted)
            formatTime((DungeonUtils.bossEntryTime ?: currentTime) - DungeonUtils.dungeonStartTime !!)
        else "?"

        for ((name, split) in currentFloorSplits.toMap().takeUnless { it.isEmpty() } ?: return@loop) {
            val text = when {
                split.start != null && split.end != null -> {
                    val duration = (((split.end ?: 0) - (split.start ?: 0)) / 1000.0).toFixed(2).toDouble()
                    val pbTime = ((split.pbTime ?: 0) / 1000.0).toFixed(2).toDouble()
                    val splitSuffix = if (split.isPB) {
                        val old = split.pbTimeOld?.let { (it / 1000.0).toFixed(2).toDouble() }
                        if (old != null) "&7(${old}s)" else ""
                    }
                    else "&e(${pbTime}s)"

                    "$name: ${duration}s&r $splitSuffix"
                }

                split.start != null && split.end == null -> {
                    val live = ((currentTime - (split.start ?: 0)) / 1000.0).toFixed(2)
                    "$name: ${live}s&r"
                }

                else -> continue
            }
            splitLines.add(text)
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

    @SubscribeEvent
    fun onChat(event: Chat) = with(event.component.noFormatText) {
        if (! enabled) return@onChat
        if (! LocationUtils.inDungeon) return@onChat
        val floor = LocationUtils.dungeonFloor ?: return@onChat
        val currentSplits = floorSplits[floor] ?: floorSplits[floor.replace("M", "F")] ?: return@onChat
        val pbData = personalBests.getData().dungeonSplits.getOrPut(floor) {
            mutableMapOf()
        }

        currentSplits.forEachIndexed { i, entry ->
            val split = currentFloorSplits.getOrPut(entry.name) { Split() }

            if (entry.startMatches(this) || currentSplits.getOrNull(i - 1)?.endMatches(this) == true) {
                split.start = currentTime
            }

            if (entry.endMatches(this) || entry.end == null && runEndRegex.matches(this)) {
                split.end = currentTime
                val deltaTime = (split.end ?: 0) - (split.start ?: 0)
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

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! LocationUtils.inDungeon) return
        currentTime += 50
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! enabled) return
        if (! DungeonRunSplitsElement.enabled) return
        if (mc.currentScreen is HudEditorScreen) return
        if (! LocationUtils.inDungeon) return
        DungeonRunSplitsElement.draw()
    }
}