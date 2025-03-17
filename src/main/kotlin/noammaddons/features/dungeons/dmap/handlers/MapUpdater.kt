package noammaddons.features.dungeons.dmap.handlers

import kotlinx.coroutines.*
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.world.storage.MapData
import noammaddons.features.dungeons.dmap.core.DungeonMapPlayer
import noammaddons.features.dungeons.dmap.core.map.*
import noammaddons.features.dungeons.dmap.utils.MapUtils.mapX
import noammaddons.features.dungeons.dmap.utils.MapUtils.mapZ
import noammaddons.features.dungeons.dmap.utils.MapUtils.yaw
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf

object MapUpdater {
    val playerPositions = mutableMapOf<String, DungeonMapPlayer>()

    fun updatePlayers(mapData: MapData) {
        // The order of each player's icons. Eg the first player would be "icon-0", the second "icon-1" etc
        val iconOrder = DungeonUtils.dungeonTeammates.toMutableList()
        iconOrder.add(iconOrder.removeAt(0)) // Move the first player (You) to the end
        iconOrder.removeAll(iconOrder.filter { it.isDead }) // Filter dead players since they have no icon

        val mapDecors = mapData.mapDecorations
        DungeonInfo.playerIcons.clear()

        mapDecors.forEach { (iconName, vec4b) ->
            val match = Regex("^icon-(\\d+)$").find(iconName) ?: return@forEach
            val iconNumber = match.groupValues[1].toInt()
            val iconPlayer = if (iconNumber < iconOrder.size) iconOrder[iconNumber] else return@forEach

            if (vec4b.func_176110_a().toInt() == 1 || iconPlayer.entity?.entityId == mc.thePlayer.entityId) return@forEach

            val newX = vec4b.mapX.toFloat()
            val newZ = vec4b.mapZ.toFloat()
            val newYaw = vec4b.yaw

            // Retrieve or create a smooth-tracking player position
            val mapPlayer = playerPositions.getOrPut(iconPlayer.name) {
                DungeonMapPlayer(iconPlayer, iconPlayer.locationSkin).apply {
                    mapX = newX
                    mapZ = newZ
                    yaw = newYaw
                }
            }

            // Smoothly interpolate position instead of snapping
            CoroutineScope(Dispatchers.Default).launch {
                smoothUpdatePlayer(mapPlayer, newX, newZ, newYaw)
            }

            iconPlayer.mapIcon = mapPlayer
            DungeonInfo.playerIcons[iconPlayer.name] = mapPlayer
        }
    }

    val playerJobs = mutableMapOf<String, Job>()

    suspend fun smoothUpdatePlayer(player: DungeonMapPlayer, targetX: Float, targetZ: Float, targetYaw: Float) {
        // Cancel any existing movement job for this player to prevent stacking
        playerJobs[player.teammate.name]?.cancel()

        // Start a new job
        playerJobs[player.teammate.name] = CoroutineScope(Dispatchers.Default).launch {
            val startX = player.mapX
            val startZ = player.mapZ
            val startYaw = player.yaw

            var progress = 0f
            while (progress < 1f) {
                delay(25)
                progress += 0.2f

                player.mapX = MathUtils.interpolate(startX, targetX, progress).toFloat()
                player.mapZ = MathUtils.interpolate(startZ, targetZ, progress).toFloat()
                player.yaw = MathUtils.interpolateYaw(startYaw, targetYaw, progress)
            }

            // Ensure final position is exact
            player.mapX = targetX
            player.mapZ = targetZ
            player.yaw = targetYaw
        }
    }


    fun updateRooms(mapData: MapData) {
        if (LocationUtils.inBoss) return
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
