package com.github.noamm9.features.impl.dungeon.waypoints

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.hideIf
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.google.gson.reflect.TypeToken
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.io.File
import java.io.FileReader
import java.io.FileWriter
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

    data class DungeonWaypoint(val pos: BlockPos, val color: Color, val filled: Boolean, val outline: Boolean, val phase: Boolean)
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

    private val configFile = File("config/$MOD_NAME/dungeonWaypoints.json")
    private val secretPositions by lazy { ScanUtils.roomList.associate { it.name to it.secretCoords } }
    val waypoints = mutableMapOf<String, List<DungeonWaypoint>>()
    val currentRoomWaypoints = CopyOnWriteArrayList<DungeonWaypoint>()
    private val currentSecrets = CopyOnWriteArrayList<SecretWaypoint>()

    override fun init() {
        loadConfig()

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

            for (wp in waypoints) {
                Render3D.renderBlock(
                    event.ctx, wp.pos, wp.color,
                    outline = wp.outline, fill = wp.filled, phase = wp.phase
                )
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

    private fun loadConfig() = catch {
        FileReader(configFile).use { reader ->
            val type = object: TypeToken<MutableMap<String, List<DungeonWaypoint>>>() {}.type
            val loadedData = JsonUtils.gsonBuilder.fromJson<MutableMap<String, List<DungeonWaypoint>>>(reader, type) ?: return@catch
            waypoints.putAll(loadedData)
        }
    }


    fun saveConfig() = catch {
        configFile.parentFile?.mkdirs()
        FileWriter(configFile).use { writer ->
            JsonUtils.gsonBuilder.toJson(waypoints, writer)
        }
        NoammAddons.logger.info("${this.javaClass.simpleName} Config saved.")
    }


    fun saveWaypoint(absPos: BlockPos, relPos: BlockPos, roomName: String, color: Color, filled: Boolean, outline: Boolean, phase: Boolean) {
        val newWaypoint = DungeonWaypoint(relPos, color, filled, outline, phase)
        val absWaypoint = newWaypoint.copy(pos = absPos)

        waypoints.compute(roomName) { _, list ->
            val mutableList = list?.toMutableList() ?: mutableListOf()
            val replaced = mutableList.removeIf { it.pos == relPos }
            mutableList.add(newWaypoint)

            if (replaced) ChatUtils.modMessage("§e$roomName: Waypoint updated at ${absPos.toShortString()}.")
            else ChatUtils.modMessage("§a$roomName: Waypoint added at ${absPos.toShortString()}.")

            mutableList
        }

        saveConfig()

        currentRoomWaypoints.removeIf { it.pos == absPos }
        currentRoomWaypoints.add(absWaypoint)
    }
}