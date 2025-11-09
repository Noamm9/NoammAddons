package noammaddons.features.impl.dungeons.dmap.handlers

import kotlinx.coroutines.*
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.world.storage.MapData
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapPlayer
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.mapX
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.mapZ
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.yaw
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf
import java.util.concurrent.ConcurrentHashMap

object MapUpdater {
    private val playerHeadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val playerJobs = ConcurrentHashMap<String, Job>()

    fun updatePlayers(mapData: MapData) {
        if (DungeonUtils.dungeonTeammates.isEmpty()) return
        val mapDecorations = mapData.mapDecorations.entries.toList()
        val teammates = DungeonUtils.dungeonTeammates.filterNot { it.isDead }

        teammates.forEach { teammate ->
            val vec4b = mapDecorations.find { it.key == teammate.mapIcon.icon }?.value ?: return@forEach
            smoothUpdatePlayer(teammate.mapIcon, vec4b.mapX, vec4b.mapZ, vec4b.yaw)
        }
    }

    fun onPlayerDeath() {
        playerJobs.forEach { it.value.cancel() }
        playerJobs.clear()
    }

    private fun smoothUpdatePlayer(player: DungeonMapPlayer, targetX: Float, targetZ: Float, targetYaw: Float) {
        if (player.mapX == 0f && player.mapZ == 0f && player.yaw == 0f) {
            player.mapX = targetX
            player.mapZ = targetZ
            player.yaw = targetYaw
            return
        }

        if (player.mapX == targetX && player.mapZ == targetZ && player.yaw == targetYaw) {
            playerJobs.remove(player.teammate.name)?.cancel()
            return
        }

        playerHeadScope.launch {
            val oldJob = playerJobs.put(player.teammate.name, this.coroutineContext.job)
            oldJob?.cancelAndJoin()

            val startX = player.mapX
            val startZ = player.mapZ
            val startYaw = player.yaw

            val animationDuration = 350L
            val startTime = System.currentTimeMillis()
            var progress = 0f

            while (progress < 1f && isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                progress = (elapsedTime.toFloat() / animationDuration).coerceAtMost(1f)

                player.mapX = MathUtils.lerp(startX, targetX, progress).toFloat()
                player.mapZ = MathUtils.lerp(startZ, targetZ, progress).toFloat()
                player.yaw = MathUtils.interpolateYaw(startYaw, targetYaw, progress)

                delay(10)
            }

            if (isActive) {
                player.mapX = targetX
                player.mapZ = targetZ
                player.yaw = targetYaw
            }
        }
    }

    fun updateRooms(mapData: MapData) {
        if (LocationUtils.inBoss) return
        if (DungeonUtils.dungeonEnded) return
        if (DungeonUtils.thePlayer?.isDead == true) return
        HotbarMapColorParser.updateMap(mapData)

        for (x in 0 .. 10) {
            for (z in 0 .. 10) {
                val idx = z * 11 + x
                val room = DungeonInfo.dungeonList[idx]
                val mapTile = HotbarMapColorParser.getTile(x, z)

                if (room is Unknown) {
                    DungeonInfo.dungeonList[idx] = mapTile
                    if (mapTile is Room) {
                        val connected = HotbarMapColorParser.getConnected(x, z)
                        connected.firstOrNull { it.data.name != "Unknown" }?.let {
                            mapTile.addToUnique(z, x, it.data.name)
                        }
                    }
                    continue
                }

                if (mapTile.state.ordinal < room.state.ordinal) {
                    room.state = mapTile.state
                }

                if (mapTile is Room && room is Room && mapTile.data.type != room.data.type) {
                    if (room.data.name == mapTile.data.name) room.data = mapTile.data
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
                        val chunk = mc.theWorld.getChunkFromChunkCoords(room.x shr 4, room.z shr 4)
                        if (chunk.isLoaded) {
                            if (chunk.getBlockState(BlockPos(room.x, 69, room.z)).block == Blocks.air) {
                                room.opened = true
                            }
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
                            else room.opened = true
                        }
                    }
                }
            }
        }
    }
}
