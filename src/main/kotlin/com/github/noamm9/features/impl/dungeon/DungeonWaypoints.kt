package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.config.PogObject
import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.ui.gui.DungeonWaypointScreen
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.*


object DungeonWaypoints: Feature("Add a custom waypoint with /ndw add while looking at a block"), ICommandProvider {
    private val secretWaypoints by ToggleSetting("Secret Waypoints").section("Secret Waypoints")
    private val mode by DropdownSetting("Mode", 0, listOf("Fill", "Outline", "Filled Outline")).showIf { secretWaypoints.value }
    private val phase by ToggleSetting("See Through Walls", true).showIf { secretWaypoints.value }
    private val opacity by SliderSetting("Opacity", 40, 0, 100, 1).hideIf { mode.value == 1 || ! secretWaypoints.value }
    private val lineWidth by SliderSetting("Line Width", 1.5f, 1f, 10f, 0.1f).hideIf { mode.value == 0 || ! secretWaypoints.value }

    private val chestWaypoints by ToggleSetting("Chest Waypoints", true).showIf { secretWaypoints.value }.section("Toggles")
    private val itemWaypoints by ToggleSetting("Item Waypoints", true).showIf { secretWaypoints.value }
    private val batWaypoints by ToggleSetting("Bat Waypoints", true).showIf { secretWaypoints.value }
    private val essenceWaypoints by ToggleSetting("Essence Waypoints", true).showIf { secretWaypoints.value }
    private val keyWaypoints by ToggleSetting("Redstone Key Waypoints", true).showIf { secretWaypoints.value }
    private val leverWaypoints by ToggleSetting("Lever Waypoints", true).showIf { secretWaypoints.value }

    private val chestColor by ColorSetting("Chest Color", Color.MAGENTA, false).showIf { chestWaypoints.value && secretWaypoints.value }.section("Colors")
    private val itemColor by ColorSetting("Item Color", Utils.favoriteColor, false).showIf { itemWaypoints.value && secretWaypoints.value }
    private val batColor by ColorSetting("Bat Color", Color.GREEN, false).showIf { batWaypoints.value && secretWaypoints.value }
    private val essenceColor by ColorSetting("Essence Color", Color.BLACK, false).showIf { essenceWaypoints.value && secretWaypoints.value }
    private val keyColor by ColorSetting("Redstone Key Color", Color.RED, false).showIf { keyWaypoints.value && secretWaypoints.value }
    private val leverColor by ColorSetting("Lever Color", Color.YELLOW, false).showIf { leverWaypoints.value && secretWaypoints.value }

    private val storage = PogObject("dungeonWaypoints", mutableMapOf<String, MutableSet<DungeonWaypoint>>())
    private val roomWaypoints = ConcurrentHashMap<UniqueRoom, MutableSet<DungeonWaypoint>>()
    private val doneSecrets = ConcurrentHashMap<UniqueRoom, MutableSet<BlockPos>>()
    private val bossWaypoints = ConcurrentHashMap.newKeySet<DungeonWaypoint>()

    override fun init() {
        register<DungeonEvent.RoomEvent.RotationFound> {
            val stored = storage.get()[event.room.name].orEmpty().map {
                it.copy(pos = ScanUtils.getRealCoord(it.pos, event.corner, 360 - event.rotation))
            }

            roomWaypoints[event.room] = stored.toCollection(ConcurrentHashMap.newKeySet())
        }

        register<DungeonEvent.BossEnterEvent> {
            val (name) = getRoomData() ?: return@register
            storage.get()[name]?.let(bossWaypoints::addAll)
        }

        register<DungeonEvent.SecretEvent> {
            if (! secretWaypoints.value) return@register
            val room = ScanUtils.currentRoom ?: return@register
            val secrets = room.secretCoords[event.type] ?: return@register

            val special = setOf(SecretType.BAT, SecretType.ITEM)
            val target = if (event.type !in special) secrets.find { it == event.pos }
            else {
                val maxDistance = when (event.type) {
                    SecretType.ITEM -> 25
                    SecretType.BAT -> 144
                    else -> Int.MAX_VALUE
                }

                secrets.asSequence()
                    .map { it to it.distSqr(event.pos) }
                    .minByOrNull { it.second }
                    ?.takeIf { it.second <= maxDistance }
                    ?.first
            }

            target?.let { doneSecrets.getOrPut(room) { ConcurrentHashMap.newKeySet() }.add(it) }
        }

        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon) return@register

            if (LocationUtils.inBoss) {
                for (wp in bossWaypoints) event.ctx.renderBlock(
                    wp.pos, wp.color, outline = wp.outline,
                    fill = wp.filled, phase = wp.phase
                )
                return@register
            }

            val room = ScanUtils.currentRoom ?: return@register
            val currentWaypoints = roomWaypoints[room] ?: emptySet()

            for (wp in currentWaypoints) event.ctx.renderBlock(
                wp.pos, wp.color, outline = wp.outline,
                fill = wp.filled, phase = wp.phase
            )

            if (! secretWaypoints.value) return@register
            if (room.mainRoom.state == RoomState.GREEN) return@register

            val done = doneSecrets[room] ?: emptySet()
            for ((type, positions) in room.secretCoords) {
                if (! type.isEnabled()) continue

                for (pos in positions) {
                    if (pos in done) continue

                    if (type == SecretType.REDSTONE_KEY && WorldUtils.getBlockAt(pos) != Blocks.PLAYER_HEAD) continue
                    val alpha = if (mode.value.equalsOneOf(0, 2)) {
                        (opacity.value * 2.55).toInt()
                    } else { 255 }
                    event.ctx.renderBlock(
                        pos, type.color().withAlpha(alpha),
                        mode.value.equalsOneOf(1, 2),
                        mode.value.equalsOneOf(0, 2),
                        phase = phase.value,
                        lineWidth = lineWidth.value
                    )
                }
            }
        }

        register<WorldChangeEvent> {
            roomWaypoints.clear()
            bossWaypoints.clear()
            doneSecrets.clear()
        }
    }

    override fun CommandBuilder.command() {
        setName("ndw")
        description("Manage custom dungeon waypoints")
        requires("Enable the $name Feature to use /ndw.") { enabled }
        runs { ChatUtils.modMessage("&bUsage: /ndw <add|edit|remove|clear>") }

        literal("add") {
            runs {
                val info = getRoomData() ?: return@runs
                val currentWaypoints = info.waypoints() ?: return@runs ChatUtils.modMessage("§cRoom rotation not scanned yet, try again in a moment.")
                val lookingAt = PlayerUtils.getSelectionBlock() ?: return@runs ChatUtils.modMessage("§cYou must be looking at a block!")
                if (currentWaypoints.any { it.pos == lookingAt }) return@runs ChatUtils.modMessage("§cA waypoint already exists here. Use /ndw edit.")

                val relativePos = ScanUtils.getRelativeCoord(lookingAt, info.corner, info.rotation)
                GuiUtils.setScreen(DungeonWaypointScreen(info.name, lookingAt, relativePos))
            }
        }

        literal("edit") {
            runs {
                val info = getRoomData() ?: return@runs
                val currentWaypoints = info.waypoints() ?: return@runs ChatUtils.modMessage("§cRoom rotation not scanned yet, try again in a moment.")
                val lookingAt = PlayerUtils.getSelectionBlock() ?: return@runs ChatUtils.modMessage("§cYou must be looking at a block!")
                val existing = currentWaypoints.find { it.pos == lookingAt } ?: return@runs ChatUtils.modMessage("§cNo waypoint found at that block.")

                val relativePos = ScanUtils.getRelativeCoord(lookingAt, info.corner, info.rotation)
                GuiUtils.setScreen(DungeonWaypointScreen(info.name, lookingAt, relativePos, existing))
            }
        }

        literal("remove") {
            runs {
                val info = getRoomData() ?: return@runs
                val currentWaypoints = info.waypoints() ?: return@runs ChatUtils.modMessage("§cRoom rotation not scanned yet, try again in a moment.")
                val lookingAt = PlayerUtils.getSelectionBlock() ?: return@runs ChatUtils.modMessage("§cYou must be looking at a block!")
                val waypoints = storage.get()

                val closest = currentWaypoints.find { it.pos == lookingAt } ?: return@runs ChatUtils.modMessage("§cNo waypoints found in this room.")

                val relativePosToRemove = ScanUtils.getRelativeCoord(closest.pos, info.corner, info.rotation)
                val roomList = waypoints.getOrDefault(info.name, mutableSetOf())
                if (roomList.removeIf { it.pos == relativePosToRemove }) {
                    waypoints[info.name] = roomList
                    currentWaypoints.removeIf { it.pos == closest.pos }
                    ChatUtils.modMessage("§aWaypoint removed.")
                }
                else ChatUtils.modMessage("§cError syncing config.")
            }
        }

        literal("clear") {
            runs {
                val info = getRoomData() ?: return@runs
                val currentWaypoints = info.waypoints() ?: return@runs ChatUtils.modMessage("§cRoom rotation not scanned yet, try again in a moment.")
                if (currentWaypoints.isEmpty()) return@runs ChatUtils.modMessage("§cNo waypoints set for this room.")
                storage.get().remove(info.name)
                currentWaypoints.clear()
                ChatUtils.modMessage("§aAll waypoints cleared for room: ${info.name}")
            }
        }
    }

    private fun getRoomData(): RoomInfo? {
        val floor = LocationUtils.dungeonFloorNumber ?: run {
            ChatUtils.modMessage("§cYou must be in a dungeon to edit waypoints!")
            return null
        }

        if (LocationUtils.inBoss) return RoomInfo("B$floor", BlockPos.ZERO, 0, null)

        val currentRoom = ScanUtils.currentRoom ?: run {
            ChatUtils.modMessage("§cYou must be in a dungeon room to edit waypoints!")
            return null
        }

        return RoomInfo(
            currentRoom.name,
            currentRoom.clayPos ?: BlockPos.ZERO,
            360 - (currentRoom.rotation ?: 0),
            currentRoom
        )
    }

    fun saveWaypoint(absPos: BlockPos, relPos: BlockPos, roomName: String, color: Color, filled: Boolean, outline: Boolean, phase: Boolean) {
        val newWaypoint = DungeonWaypoint(relPos, color, filled, outline, phase)
        val absWaypoint = newWaypoint.copy(pos = absPos)

        storage.get().compute(roomName) { _, list ->
            val set = list ?: mutableSetOf()
            val replaced = set.removeIf { it.pos == relPos }
            set.add(newWaypoint)

            if (replaced) ChatUtils.modMessage("§e$roomName: Waypoint updated at ${absPos.toShortString()}.")
            else ChatUtils.modMessage("§a$roomName: Waypoint added at ${absPos.toShortString()}.")

            set
        }

        if (LocationUtils.inBoss) {
            bossWaypoints.removeIf { it.pos == absPos }
            bossWaypoints.add(absWaypoint)
            return
        }

        for ((room, set) in roomWaypoints) {
            if (room.name != roomName) continue
            set.removeIf { it.pos == absPos }
            set.add(absWaypoint)
        }
    }

    private fun SecretType.isEnabled() = when (this) {
        SecretType.CHEST -> chestWaypoints.value
        SecretType.ITEM -> itemWaypoints.value
        SecretType.BAT -> batWaypoints.value
        SecretType.WITHER_ESSENCE -> essenceWaypoints.value
        SecretType.REDSTONE_KEY -> keyWaypoints.value
        SecretType.LEVER -> leverWaypoints.value
    }

    private fun SecretType.color() = when (this) {
        SecretType.REDSTONE_KEY -> keyColor
        SecretType.WITHER_ESSENCE -> essenceColor
        SecretType.CHEST -> chestColor
        SecretType.ITEM -> itemColor
        SecretType.BAT -> batColor
        SecretType.LEVER -> leverColor
    }.value

    private fun RoomInfo.waypoints() = run { roomWaypoints[room ?: return@run bossWaypoints] }
    data class DungeonWaypoint(val pos: BlockPos, val color: Color, val filled: Boolean, val outline: Boolean, val phase: Boolean)
    private data class RoomInfo(val name: String, val corner: BlockPos, val rotation: Int, val room: UniqueRoom?)
}