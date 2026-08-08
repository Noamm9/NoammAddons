package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.gui.DungeonWaypointScreen
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D.renderBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.*

private typealias RoomInfo = Triple<String, BlockPos, Int>

object DungeonWaypoints: Feature("Add a custom waypoint with /ndw add while looking at a block"), ICommandProvider {
    private val secretWaypoints by ToggleSetting("Secret Waypoints").section("Secret Waypoints")
    private val mode by DropdownSetting("Mode", 0, listOf("Fill", "Outline", "Filled Outline"))
    private val phase by ToggleSetting("See Through Walls", true)
    private val opacity by SliderSetting("Opacity", 40, 0, 100, 1).hideIf { mode.value == 1 }
    private val lineWidth by SliderSetting("Line Width", 1.5f, 1f, 10f, 0.1f).hideIf { mode.value == 0 }

    private val chestColor by ColorSetting("Chest Color", Color.MAGENTA, false).section("Colors")
    private val itemColor by ColorSetting("Item Color", Utils.favoriteColor, false)
    private val batColor by ColorSetting("Bat Color", Color.GREEN, false)
    private val essenceColor by ColorSetting("Essence Color", Color.BLACK, false)
    private val keyColor by ColorSetting("Redstone Key Color", Color.RED, false)

    data class DungeonWaypoint(val pos: BlockPos, val color: Color, val filled: Boolean, val outline: Boolean, val phase: Boolean)
    private data class SecretWaypoint(val pos: BlockPos, val type: SecretType) {
        val color = when (type) {
            SecretType.REDSTONE_KEY -> keyColor
            SecretType.WITHER_ESSENCE -> essenceColor
            SecretType.CHEST -> chestColor
            SecretType.ITEM -> itemColor
            SecretType.BAT -> batColor
            else -> chestColor
        }.value
    }

    private val waypoints = PogObject("dungeonWaypoints", mutableMapOf<String, MutableList<DungeonWaypoint>>())
    private val currentWaypoints = CopyOnWriteArrayList<DungeonWaypoint>()
    private val currentSecrets = CopyOnWriteArrayList<SecretWaypoint>()

    override fun init() {
        register<DungeonEvent.RoomEvent.onEnter> {
            currentWaypoints.clear()
            currentSecrets.clear()

            val (roomName, roomCorner, roomRotation) = getRoomData() ?: return@register

            waypoints.get()[roomName]?.map { wp ->
                wp.copy(pos = ScanUtils.getRealCoord(wp.pos, roomCorner, roomRotation))
            }?.let { currentWaypoints.addAll(it) }

            if (! secretWaypoints.value) return@register
            if (event.room.mainRoom.state == RoomState.GREEN) return@register
            val coords = ScanUtils.secretMap[roomName] ?: return@register

            val activeSecrets = buildList {
                fun addSecrets(list: List<BlockPos>, type: SecretType) = list.forEach {
                    add(SecretWaypoint(ScanUtils.getRealCoord(it, roomCorner, roomRotation), type))
                }

                addSecrets(coords.redstoneKey, SecretType.REDSTONE_KEY)
                addSecrets(coords.wither, SecretType.WITHER_ESSENCE)
                addSecrets(coords.bat, SecretType.BAT)
                addSecrets(coords.item, SecretType.ITEM)
                addSecrets(coords.chest, SecretType.CHEST)
            }

            currentSecrets.addAll(activeSecrets)
        }

        register<DungeonEvent.BossEnterEvent> {
            currentWaypoints.clear()
            currentSecrets.clear()
            ThreadUtils.scheduledTask(10) {
                currentWaypoints.addAll(waypoints.get()["B${LocationUtils.dungeonFloorNumber}"].orEmpty())
            }
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
            if (! LocationUtils.inDungeon) return@register

            for (wp in currentWaypoints) event.ctx.renderBlock(
                wp.pos, wp.color, outline = wp.outline,
                fill = wp.filled, phase = wp.phase
            )

            if (! secretWaypoints.value) return@register
            if (ScanUtils.currentRoom?.mainRoom?.state == RoomState.GREEN) return@register
            if (LocationUtils.inBoss) return@register

            for (wp in currentSecrets) {
                if (wp.type == SecretType.REDSTONE_KEY && WorldUtils.getBlockAt(wp.pos) != Blocks.PLAYER_HEAD) continue
                event.ctx.renderBlock(
                    wp.pos, wp.color.withAlpha((opacity.value * 2.55).toInt()),
                    mode.value.equalsOneOf(1, 2),
                    mode.value.equalsOneOf(0, 2),
                    phase = phase.value,
                    lineWidth = lineWidth.value
                )
            }
        }

        register<WorldChangeEvent> {
            currentSecrets.clear()
            currentWaypoints.clear()
        }
    }

    override fun CommandBuilder.command() {
        setName("ndw")
        runs { ChatUtils.modMessage("&bUsage: /ndw <add|edit|remove|clear>") }

        literal("add") {
            runs {
                val (roomName, roomCorner, rotation) = getRoomData() ?: return@runs
                val lookingAt = PlayerUtils.getSelectionBlock() ?: return@runs ChatUtils.modMessage("§cYou must be looking at a block!")
                if (currentWaypoints.any { it.pos == lookingAt }) return@runs ChatUtils.modMessage("§cA waypoint already exists here. Use /ndw edit.")

                val relativePos = ScanUtils.getRelativeCoord(lookingAt, roomCorner, rotation)
                GuiUtils.setScreen(DungeonWaypointScreen(roomName, lookingAt, relativePos))
            }
        }

        literal("edit") {
            runs {
                val (roomName, roomCorner, rotation) = getRoomData() ?: return@runs
                val lookingAt = PlayerUtils.getSelectionBlock() ?: return@runs ChatUtils.modMessage("§cYou must be looking at a block!")
                val existing = currentWaypoints.find { it.pos == lookingAt } ?: return@runs ChatUtils.modMessage("§cNo waypoint found at that block.")

                val relativePos = ScanUtils.getRelativeCoord(lookingAt, roomCorner, rotation)
                GuiUtils.setScreen(DungeonWaypointScreen(roomName, lookingAt, relativePos, existing))
            }
        }

        literal("remove") {
            runs {
                val (roomName, roomCorner, rotation) = getRoomData() ?: return@runs
                val lookingAt = PlayerUtils.getSelectionBlock() ?: return@runs ChatUtils.modMessage("§cYou must be looking at a block!")
                val waypoints = waypoints.get()

                val closest = (if (roomName.startsWith("B")) waypoints[roomName] else currentWaypoints)?.find { it.pos == lookingAt }
                    ?: return@runs ChatUtils.modMessage("§cNo waypoints found in this room.")

                val relativePosToRemove = ScanUtils.getRelativeCoord(closest.pos, roomCorner, rotation)
                val roomList = waypoints.getOrDefault(roomName, emptyList()).toMutableList()
                if (roomList.removeIf { it.pos == relativePosToRemove }) {
                    waypoints[roomName] = roomList
                    currentWaypoints.remove(closest)
                    ChatUtils.modMessage("§aWaypoint removed.")
                }
                else ChatUtils.modMessage("§cError syncing config.")
            }
        }

        literal("clear") {
            runs {
                val (roomName, _, _) = getRoomData() ?: return@runs
                if (currentWaypoints.isEmpty()) return@runs ChatUtils.modMessage("§cNo waypoints set for this room.")
                waypoints.get().remove(roomName)
                currentWaypoints.clear()
                ChatUtils.modMessage("§aAll waypoints cleared for room: $roomName")
            }
        }
    }


    private fun getRoomData(): RoomInfo? {
        val floor = LocationUtils.dungeonFloorNumber ?: run {
            ChatUtils.modMessage("§cYou must be in a dungeon to edit waypoints!")
            return null
        }

        if (LocationUtils.inBoss) return RoomInfo("B$floor", BlockPos.ZERO, 0)

        val currentRoom = ScanUtils.currentRoom ?: run {
            ChatUtils.modMessage("§cYou must be in a dungeon room to edit waypoints!")
            return null
        }

        return RoomInfo(
            currentRoom.name,
            currentRoom.clayPos ?: BlockPos.ZERO,
            360 - (currentRoom.rotation ?: 0)
        )
    }


    fun saveWaypoint(absPos: BlockPos, relPos: BlockPos, roomName: String, color: Color, filled: Boolean, outline: Boolean, phase: Boolean) {
        val newWaypoint = DungeonWaypoint(relPos, color, filled, outline, phase)
        val absWaypoint = newWaypoint.copy(pos = absPos)

        waypoints.get().compute(roomName) { _, list ->
            val mutableList = list ?: mutableListOf()
            val replaced = mutableList.removeIf { it.pos == relPos }
            mutableList.add(newWaypoint)

            if (replaced) ChatUtils.modMessage("§e$roomName: Waypoint updated at ${absPos.toShortString()}.")
            else ChatUtils.modMessage("§a$roomName: Waypoint added at ${absPos.toShortString()}.")

            mutableList
        }

        currentWaypoints.removeIf { it.pos == absPos }
        currentWaypoints.add(absWaypoint)
    }
}