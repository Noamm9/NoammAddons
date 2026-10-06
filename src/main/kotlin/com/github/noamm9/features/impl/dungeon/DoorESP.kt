package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dungeon.map.DungeonMap
import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.handlers.DungeonTree
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.world.Render3D.renderString
import net.minecraft.ChatFormatting
import java.awt.Color

object DoorESP: Feature(
    //#if LEGIT
    //$name = "Door Highlight",
    //#endif
    jsonName = "Door ESP",
    description = "Highlights every door in the dungeon run during clear."
) {
    private val brDoors by ToggleSetting("Blood rush doors")
    private val normalDoors by ToggleSetting("Normal doors")
    private val normalDoorColor by ColorSetting("Normal Door", Color.WHITE.withAlpha(50)).showIf { normalDoors.value }
    private val doorNoKeyColor by ColorSetting("No Key Color ", Color.RED.withAlpha(50)).showIf { brDoors.value }
    private val doorKeyColor by ColorSetting("Has Key Color ", Color.GREEN.withAlpha(50)).showIf { brDoors.value }
    private val roomName by ToggleSetting("Room Name").withDescription("shows the name of the room the door leads to").section("Extra")
    private val roomSecrets by ToggleSetting("Room Secrets").withDescription("shows the secrets of room the doors leads to")

    private val mode by DropdownSetting("Mode", 0, listOf("Outline", "Fill", "Filled Outline")).section("Options")
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase").withDescription("Toggles phase for ${roomName.name}, ${roomSecrets.name} aswell")

    override fun init() {
        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
            val currentRoom = ScanUtils.currentRoom ?: return@register

            for (tile in DungeonScanner.dungeonList) {
                if (tile !is DoorTile) continue
                if (tile.type == DoorType.ENTRANCE) continue
                if (DungeonMap.enabled && ! MapConfig.dungeonMapCheater.value) {
                    if (tile.state == RoomState.UNDISCOVERED && ! DungeonTree.isFairy(tile)) continue
                }

                val tiles = tile.roomTiles.mapNotNull(RoomTile::uniqueRoom)
                if (currentRoom !in tiles) continue
                var brDoor = false

                val color = if (tile.type.equalsOneOf(DoorType.BLOOD, DoorType.WITHER) && ! tile.opened && DungeonListener.bloodOpenTime == null) {
                    brDoor = true
                    if (brDoors.value) (if (tile.type.keys > 0) doorKeyColor else doorNoKeyColor).value else return@register
                }
                else if (normalDoors.value) normalDoorColor.value else return@register

                event.ctx.renderBoxBounds(
                    tile.aabb, color,
                    outline = mode.value == 0 || mode.value == 2,
                    fill = mode.value == 1 || mode.value == 2,
                    phase = phase.value,
                    lineWidth = lineWidth.value
                )

                if (brDoor) continue
                val nextRoom = tiles.find { it != currentRoom } ?: continue

                var width = 0
                val text = buildString {
                    if (roomName.value) {
                        val color = when (nextRoom.mainRoom.state) {
                            RoomState.GREEN -> ChatFormatting.GREEN
                            RoomState.CLEARED -> ChatFormatting.WHITE
                            RoomState.FAILED -> ChatFormatting.RED
                            else -> ChatFormatting.WHITE
                        }.toString()

                        val str = color + nextRoom.name
                        width = maxOf(width, str.width())
                        append(str)

                    }

                    if (roomSecrets.value && nextRoom.data.secrets > 0) {
                        val color = ColorUtils.colorCodeByPercent(nextRoom.foundSecrets, nextRoom.data.secrets)
                        val str = "$color${nextRoom.foundSecrets}&f/&a${nextRoom.data.secrets}"
                        width = maxOf(width, str.width())
                        append("\n$str")
                    }
                }.ifEmpty { continue }

                var scale = 2f
                val ref = 0.025f
                var worldwidth = width * ref * scale

                while (worldwidth > 2.8f) {
                    scale -= 0.01f
                    worldwidth = width * ref * scale
                }

                val phase = run {
                    if (! phase.value) return@run false
                    val state = nextRoom.mainRoom.state

                    if (DungeonMap.enabled && ! MapConfig.dungeonMapCheater.value) {
                        if (
                            state.equalsOneOf(RoomState.UNDISCOVERED, RoomState.UNOPENED)
                            && ! DungeonTree.isFairy(tile)
                        ) return@run false
                    }

                    return@run true
                }

                event.ctx.renderString(text, tile.aabb.center, scale = scale, phase = phase)
            }
        }
    }
}