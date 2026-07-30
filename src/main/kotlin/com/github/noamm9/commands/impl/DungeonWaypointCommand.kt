package com.github.noamm9.commands.impl

import com.github.noamm9.NoammAddons
import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.impl.dungeon.DungeonWaypoints
import com.github.noamm9.ui.gui.DungeonWaypointScreen
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object DungeonWaypointCommand: BaseCommand("ndw") {
    override fun CommandNodeBuilder.build() {
        literal("add") {
            runs {
                val (roomName, roomCorner, rotation) = getRoomData() ?: return@runs

                val hit = NoammAddons.mc.hitResult
                if (hit == null || hit.type != HitResult.Type.BLOCK) {
                    ChatUtils.modMessage("§cYou must be looking at a block!")
                    return@runs
                }

                val lookingAt = (hit as BlockHitResult).blockPos

                if (DungeonWaypoints.currentRoomWaypoints.any { it.pos == lookingAt }) {
                    ChatUtils.modMessage("§cA waypoint already exists here. Use /ndw edit.")
                    return@runs
                }

                val relativePos = ScanUtils.getRelativeCoord(lookingAt, roomCorner, rotation)

                GuiUtils.setScreen(DungeonWaypointScreen(roomName, lookingAt, relativePos))
            }
        }

        literal("edit") {
            runs {
                val (roomName, roomCorner, rotation) = getRoomData() ?: return@runs

                val hit = NoammAddons.mc.hitResult
                if (hit == null || hit.type != HitResult.Type.BLOCK) {
                    ChatUtils.modMessage("§cYou must be looking at a block!")
                    return@runs
                }

                val lookingAt = (hit as BlockHitResult).blockPos
                val existing = (if (LocationUtils.inBoss) DungeonWaypoints.waypoints.get()["B${LocationUtils.dungeonFloorNumber}"]
                else DungeonWaypoints.currentRoomWaypoints)?.firstOrNull { it.pos == lookingAt }

                if (existing == null) {
                    ChatUtils.modMessage("§cNo waypoint found at that block.")
                    return@runs
                }

                val relativePos = ScanUtils.getRelativeCoord(lookingAt, roomCorner, rotation)
                NoammAddons.mc.setScreen(DungeonWaypointScreen(roomName, lookingAt, relativePos, existing))
            }
        }

        literal("remove") {
            runs {
                val (roomName, roomCorner, rotation) = getRoomData() ?: return@runs
                val playerPos = NoammAddons.mc.player?.position() ?: return@runs
                val waypoints = DungeonWaypoints.waypoints.get()

                val closest = (if (LocationUtils.inBoss) waypoints[roomName] else DungeonWaypoints.currentRoomWaypoints)?.minByOrNull {
                    val dx = it.pos.x + 0.5 - playerPos.x
                    val dy = it.pos.y + 0.5 - playerPos.y
                    val dz = it.pos.z + 0.5 - playerPos.z
                    dx * dx + dy * dy + dz * dz
                }

                if (closest == null) return@runs ChatUtils.modMessage("§cNo waypoints found in this room.")

                val distSq = (closest.pos.x + 0.5 - playerPos.x).let { x ->
                    x * x + (closest.pos.y + 0.5 - playerPos.y).let { y ->
                        y * y + (closest.pos.z + 0.5 - playerPos.z).let { z -> z * z }
                    }
                }

                if (distSq >= 25.0) return@runs ChatUtils.modMessage("§cNo waypoint found nearby (must be within 5 blocks).")

                val relativePosToRemove = ScanUtils.getRelativeCoord(closest.pos, roomCorner, rotation)
                val roomList = waypoints.getOrDefault(roomName, emptyList()).toMutableList()
                if (roomList.removeIf { it.pos == relativePosToRemove }) {
                    waypoints[roomName] = roomList
                    DungeonWaypoints.currentRoomWaypoints.remove(closest)
                    ChatUtils.modMessage("§aWaypoint removed.")
                }
                else ChatUtils.modMessage("§cError syncing config.")
            }
        }

        literal("clear") {
            runs {
                val (roomName, _, _) = getRoomData() ?: return@runs

                if (DungeonWaypoints.currentRoomWaypoints.isEmpty()) {
                    ChatUtils.modMessage("§cNo waypoints set for this room.")
                    return@runs
                }

                DungeonWaypoints.waypoints.get().remove(roomName)
                DungeonWaypoints.currentRoomWaypoints.clear()
                ChatUtils.modMessage("§aAll waypoints cleared for room: $roomName")
            }
        }
    }


    private data class RoomInfo(val name: String, val corner: BlockPos, val rotation: Int)

    private fun getRoomData(): RoomInfo? {
        val floor = LocationUtils.dungeonFloorNumber
        if (floor == null) {
            ChatUtils.modMessage("§cYou must be in a dungeon to edit waypoints!")
            return null
        }

        if (LocationUtils.inBoss) return RoomInfo(
            name = "B$floor",
            corner = BlockPos.ZERO,
            rotation = 0
        )
        else {
            val currentRoom = ScanUtils.currentRoom
            if (currentRoom == null) {
                ChatUtils.modMessage("§cYou must be in a dungeon room to edit waypoints!")
                return null
            }

            return RoomInfo(
                name = currentRoom.data.name,
                corner = currentRoom.clayPos ?: BlockPos.ZERO,
                rotation = 360 - (currentRoom.rotation ?: 0)
            )
        }
    }
}