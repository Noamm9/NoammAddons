package noammaddons.features.impl.dungeons.dmap.handlers

import kotlinx.coroutines.*
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.world.storage.MapData
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapPlayer
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.mapX
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.mapZ
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.yaw
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf

object MapUpdater {
    val playerJobs = mutableMapOf<String, Job>()

    fun updatePlayers(mapData: MapData) {
        if (DungeonUtils.dungeonTeammates.isEmpty()) return
        val mapDecorations = mapData.mapDecorations.entries.toList()
        val teammates = DungeonUtils.dungeonTeammates.toList()

        teammates.forEach { teammate ->
            val (_, vec4b) = mapDecorations.find { it.key == teammate.mapIcon.icon } ?: return@forEach
            smoothUpdatePlayer(teammate.mapIcon, vec4b.mapX, vec4b.mapZ, vec4b.yaw)
        }
    }

    private fun smoothUpdatePlayer(player: DungeonMapPlayer, targetX: Float, targetZ: Float, targetYaw: Float) {
        playerJobs[player.teammate.name]?.cancel()

        playerJobs[player.teammate.name] = scope.launch {
            val startX = player.mapX
            val startZ = player.mapZ
            val startYaw = player.yaw

            if (startYaw != 0f && startX != 0f && startZ != 0f) {
                var progress = 0f
                while (progress < 1f) {
                    delay(25)
                    progress += 0.2f

                    player.mapX = MathUtils.lerp(startX, targetX, progress).toFloat()
                    player.mapZ = MathUtils.lerp(startZ, targetZ, progress).toFloat()
                    player.yaw = MathUtils.interpolateYaw(startYaw, targetYaw, progress)
                }
            }

            player.mapX = targetX
            player.mapZ = targetZ
            player.yaw = targetYaw
        }
    }


    fun updateRooms(mapData: MapData) {
        if (LocationUtils.inBoss) return
        if (DungeonUtils.dungeonEnded) return
        if (DungeonUtils.thePlayer?.isDead == true) return
        DungeonMapColorParser.updateMap(mapData)

        for (x in 0 .. 10) {
            for (z in 0 .. 10) {
                val room = DungeonInfo.dungeonList[z * 11 + x]
                val mapTile = DungeonMapColorParser.getTile(x, z)

                if (room is Unknown) {
                    DungeonInfo.dungeonList[z * 11 + x] = mapTile
                    if (mapTile is Room) {
                        val connected = DungeonMapColorParser.getConnected(x, z)
                        connected.firstOrNull { it.data.name != "Unknown" }?.let {
                            mapTile.addToUnique(z, x, it.data.name)
                        }
                    }
                    continue
                }

                if (mapTile.state.ordinal < room.state.ordinal) {
                    room.state = mapTile.state
                }

                if (mapTile is Door && room is Door) {
                    if (mapTile.type == DoorType.WITHER && room.type != DoorType.WITHER) {
                        room.type = mapTile.type
                    }
                }

                if (room is Door && room.type.equalsOneOf(DoorType.ENTRANCE, DoorType.WITHER, DoorType.BLOOD)) {
                    if (mapTile is Door && mapTile.type == DoorType.WITHER) {
                        room.opened = false
                    }
                    else if (! room.opened) {
                        val chunk = mc.theWorld.getChunkFromChunkCoords(
                            room.x shr 4,
                            room.z shr 4
                        )
                        if (chunk.isLoaded) {
                            if (chunk.getBlockState(BlockPos(room.x, 69, room.z)).block == Blocks.air)
                                room.opened = true
                        }
                        else if (mapTile is Door && mapTile.state == RoomState.DISCOVERED) {
                            if (room.type == DoorType.BLOOD) {
                                val bloodRoom = DungeonInfo.uniqueRooms.find { r ->
                                    r.mainRoom.data.type == RoomType.BLOOD
                                }

                                if (bloodRoom != null && bloodRoom.mainRoom.state != RoomState.UNOPENED) {
                                    room.opened = true
                                }
                            }
                            else {
                                room.opened = true
                            }
                        }
                    }
                }
            }
        }
    }
}
