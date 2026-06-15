package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.RouteImportParser
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.*

object DungeonWaypoints: Feature("Add a custom waypoint with /ndw add while looking at a block") {
    val secretWaypoints by ToggleSetting("Secret Waypoints").section("Secret Waypoints")
    val mode by DropdownSetting("Mode", 0, listOf("Fill", "Outline", "Filled Outline"))
    val phase by ToggleSetting("See Through Walls", true)
    val opacity by SliderSetting("Opacity", 40, 0, 100, 1).hideIf { mode.value == 1 }
    val lineWidth by SliderSetting("Line Width", 1.5f, 1f, 10f, 0.1f).hideIf { mode.value == 0 }

    val chestColor by ColorSetting("Chest Color", Color.MAGENTA, false).section("Colors")
    val itemColor by ColorSetting("Item Color", Utils.favoriteColor, false)
    val batColor by ColorSetting("Bat Color", Color.GREEN, false)
    val essanceColor by ColorSetting("Essence Color", Color.BLACK, false)
    val keyColor by ColorSetting("Redstone Key Color", Color.RED, false)

    private val importButton by ButtonSetting("Import Route (clipboard)") { importFromClipboard() }.section("Import")
    private val resetImportButton by ButtonSetting("Reset Imported Waypoints") { clearAllWaypoints() }.withDescription("Removes ALL custom waypoints (imported + manually added), leaving only the mod's built-in waypoints.")

    val titleColor by ColorSetting("Title Color", Color.WHITE, false).section("Titles").withDescription("Color of the waypoint title text.")
    val titleScale by SliderSetting("Title Size", 4f, 0.5f, 25f, 0.1f).withDescription("Text size of normal waypoint titles.")
    val startTitleScale by SliderSetting("Start Title Size", 6f, 0.5f, 25f, 0.1f).withDescription("Text size of \"start\" titles (any title containing \"start\").")
    val titleBackground by ToggleSetting("Title Background", true).withDescription("Draws a background plate behind titles for readability.")
    val titleBgOpacity by SliderSetting("Background Opacity", 65, 0, 100, 1).showIf { titleBackground.value }.withDescription("Opacity of the title background plate.")

    val customStartBox by ToggleSetting("Custom Start Box", false).section("Start Waypoint").withDescription("Give \"start\" waypoints (title containing \"start\") their own box color and outline width.")
    val startBoxColor by ColorSetting("Start Box Color", Color.WHITE, false).showIf { customStartBox.value }.withDescription("Box color for \"start\" waypoints.")
    val startBoxLineWidth by SliderSetting("Start Line Width", 4f, 1f, 10f, 0.1f).showIf { customStartBox.value }.withDescription("Outline width for \"start\" waypoints.")

    data class DungeonWaypoint(
        val pos: BlockPos, val color: Color, val filled: Boolean,
        val outline: Boolean, val phase: Boolean,
        val title: String? = null,
    )
    private data class SecretWaypoint(val pos: BlockPos, val type: SecretType) {
        val color = when (type) {
            SecretType.REDSTONE_KEY -> keyColor
            SecretType.WITHER_ESSANCE -> essanceColor
            SecretType.CHEST -> chestColor
            SecretType.ITEM -> itemColor
            SecretType.BAT -> batColor
            else -> chestColor
        }.value
    }

    private val waypointsPog = PogObject("dungeonWaypoints", mutableMapOf<String, MutableList<DungeonWaypoint>>())
    val waypoints by waypointsPog
    private val secretPositions by lazy { ScanUtils.roomList.associate { it.name to it.secretCoords } }
    val currentRoomWaypoints = CopyOnWriteArrayList<DungeonWaypoint>()
    private val currentSecrets = CopyOnWriteArrayList<SecretWaypoint>()

    override fun init() {
        register<DungeonEvent.RoomEvent.onEnter> {
            currentRoomWaypoints.clear()
            currentSecrets.clear()

            val roomName = event.room.name
            val roomRotation = 360 - (event.room.rotation ?: return@register)
            val roomCorner = event.room.corner ?: return@register

            waypoints[roomName]?.map { wp ->
                wp.copy(pos = ScanUtils.getRealCoord(wp.pos, roomCorner, roomRotation))
            }?.let { currentRoomWaypoints.addAll(it) }

            if (! secretWaypoints.value) return@register
            if (event.room.mainRoom.state == RoomState.GREEN) return@register
            val coords = secretPositions[roomName] ?: return@register

            val activeSecrets = buildList {
                fun addSecrets(list: List<BlockPos>, type: SecretType) {
                    list.forEach { add(SecretWaypoint(ScanUtils.getRealCoord(it, roomCorner, roomRotation), type)) }
                }

                addSecrets(coords.redstoneKey, SecretType.REDSTONE_KEY)
                addSecrets(coords.wither, SecretType.WITHER_ESSANCE)
                addSecrets(coords.bat, SecretType.BAT)
                addSecrets(coords.item, SecretType.ITEM)
                addSecrets(coords.chest, SecretType.CHEST)
            }

            currentSecrets.addAll(activeSecrets)
        }

        register<DungeonEvent.SecretEvent> {
            if (! secretWaypoints.value || currentSecrets.isEmpty()) return@register
            if (event.type == SecretType.LEVER) return@register

            val special = setOf(SecretType.BAT, SecretType.ITEM)
            val target = if (event.type !in special) currentSecrets.find { it.pos == event.pos }
            else {
                val maxDistance = when (event.type) {
                    SecretType.ITEM -> 25
                    SecretType.BAT -> 144
                    else -> Int.MAX_VALUE
                }

                currentSecrets.asSequence()
                    .filter { it.type == event.type }
                    .map { it to it.pos.distSqr(event.pos) }
                    .minByOrNull { it.second }
                    ?.takeIf { it.second <= maxDistance }
                    ?.first
            }

            target?.let(currentSecrets::remove)
        }

        register<RenderWorldEvent> {
            val waypoints = if (LocationUtils.inBoss) {
                currentRoomWaypoints.clear()
                waypoints["B${LocationUtils.dungeonFloorNumber}"].orEmpty()
            }
            else currentRoomWaypoints

            val titleBg = if (titleBackground.value) ((titleBgOpacity.value * 2.55).toInt() shl 24) else 0
            for (wp in waypoints) {
                val isStart = wp.title?.contains("start", ignoreCase = true) == true
                val useStartBox = isStart && customStartBox.value
                Render3D.renderBlock(
                    event.ctx, wp.pos,
                    if (useStartBox) startBoxColor.value else wp.color,
                    outline = wp.outline, fill = wp.filled, phase = wp.phase,
                    lineWidth = if (useStartBox) startBoxLineWidth.value else lineWidth.value
                )
                wp.title?.let { title ->
                    val scale = if (isStart) startTitleScale.value else titleScale.value
                    Render3D.renderString(
                        title,
                        wp.pos.x + 0.5, wp.pos.y + 0.5 + 0.1 * scale, wp.pos.z + 0.5,
                        titleColor.value, scale, wp.phase, titleBg
                    )
                }
            }

            if (! secretWaypoints.value) return@register
            if (ScanUtils.currentRoom?.mainRoom?.state == RoomState.GREEN) return@register
            if (LocationUtils.inBoss) return@register

            for (wp in currentSecrets) {
                if (wp.type == SecretType.REDSTONE_KEY && WorldUtils.getBlockAt(wp.pos) != Blocks.PLAYER_HEAD) continue
                Render3D.renderBlock(
                    event.ctx, wp.pos,
                    wp.color.withAlpha((opacity.value * 2.55).toInt()),
                    mode.value.equalsOneOf(1, 2),
                    mode.value.equalsOneOf(0, 2),
                    lineWidth = lineWidth.value,
                    phase = phase.value
                )
            }
        }

        register<WorldChangeEvent> {
            currentSecrets.clear()
            currentRoomWaypoints.clear()
        }
    }

    fun saveWaypoint(absPos: BlockPos, relPos: BlockPos, roomName: String, color: Color, filled: Boolean, outline: Boolean, phase: Boolean) {
        val newWaypoint = DungeonWaypoint(relPos, color, filled, outline, phase)
        val absWaypoint = newWaypoint.copy(pos = absPos)

        waypoints.compute(roomName) { _, list ->
            val mutableList = list ?: mutableListOf()
            val replaced = mutableList.removeIf { it.pos == relPos }
            mutableList.add(newWaypoint)

            if (replaced) ChatUtils.modMessage("§e$roomName: Waypoint updated at ${absPos.toShortString()}.")
            else ChatUtils.modMessage("§a$roomName: Waypoint added at ${absPos.toShortString()}.")

            mutableList
        }

        currentRoomWaypoints.removeIf { it.pos == absPos }
        currentRoomWaypoints.add(absWaypoint)
    }

    fun importFromClipboard() {
        val clip = NoammAddons.mc.keyboardHandler.clipboard
        val parsed = try {
            RouteImportParser.parse(clip)
        } catch (e: RouteImportParser.RouteImportException) {
            ChatUtils.modMessage("§cImport failed: ${e.message}")
            return
        }

        var roomCount = 0
        var wpCount = 0
        for ((roomName, list) in parsed) {
            val mapped = list.map {
                DungeonWaypoint(BlockPos(it.x, it.y, it.z), it.color, it.filled, it.outline, it.phase, it.title)
            }.toMutableList()
            waypoints[roomName] = mapped
            roomCount++
            wpCount += mapped.size
        }

        waypointsPog.save()
        refreshCurrentRoomWaypoints()
        ChatUtils.modMessage("§aImported §e$wpCount§a waypoints across §e$roomCount§a rooms. §7(existing waypoints in those rooms were replaced)")
    }

    fun clearAllWaypoints() {
        val count = waypoints.values.sumOf { it.size }
        if (count == 0) return ChatUtils.modMessage("§eNo custom waypoints to clear.")
        waypoints.clear()
        waypointsPog.save()
        currentRoomWaypoints.clear()
        ChatUtils.modMessage("§aCleared §e$count§a custom waypoints. Only the mod's base waypoints remain.")
    }

    private fun refreshCurrentRoomWaypoints() {
        val room = ScanUtils.currentRoom ?: return
        val rotation = 360 - (room.rotation ?: return)
        val corner = room.corner ?: return
        currentRoomWaypoints.clear()
        waypoints[room.name]
            ?.map { it.copy(pos = ScanUtils.getRealCoord(it.pos, corner, rotation)) }
            ?.let { currentRoomWaypoints.addAll(it) }
    }
}