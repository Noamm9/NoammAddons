package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.mixin.IMapState
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonListener.dungeonTeammatesNoSelf
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.utils.LegacyRegistry
import com.github.noamm9.utils.dungeons.map.utils.MapUtils.mapX
import com.github.noamm9.utils.dungeons.map.utils.MapUtils.mapZ
import com.github.noamm9.utils.dungeons.map.utils.MapUtils.yaw
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import kotlinx.coroutines.*
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import java.util.concurrent.*

object MapUpdater {
    private val playerHeadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val playerJobs = ConcurrentHashMap<String, Job>()

    fun updatePlayers() {
        val mapData = DungeonInfo.mapData as? IMapState ?: return
        val decorations = mapData.decorations ?: return
        val livingTeammates = dungeonTeammatesNoSelf.filter { ! it.isDead }

        decorations.forEach { (key, decoration) ->
            if (decoration.type.value() == MapDecorationTypes.FRAME.value()) {
                DungeonListener.thePlayer?.icon = key
            }
            else {
                val index = key.lastOrNull()?.digitToIntOrNull()
                if (index != null && index in livingTeammates.indices) {
                    livingTeammates[index].icon = key
                }
            }
        }

        DungeonListener.dungeonTeammates.forEach { teammate ->
            if (teammate.isDead) return@forEach
            val decoration = decorations[teammate.icon] ?: return@forEach
            smoothUpdatePlayer(teammate, decoration.mapX.toFloat(), decoration.mapZ.toFloat(), decoration.yaw)
        }
    }

    fun onPlayerDeath() {
        playerJobs.forEach { it.value.cancel() }
        playerJobs.clear()
    }

    private fun smoothUpdatePlayer(player: DungeonPlayer, targetX: Float, targetZ: Float, targetYaw: Float) {
        if (player.mapX == 0f && player.mapZ == 0f && player.yaw == 0f) {
            player.mapX = targetX
            player.mapZ = targetZ
            player.yaw = targetYaw
            return
        }

        if (player.mapX == targetX && player.mapZ == targetZ && player.yaw == targetYaw) {
            playerJobs.remove(player.name)?.cancel()
            return
        }

        playerHeadScope.launch {
            val oldJob = playerJobs.put(player.name, this.coroutineContext.job)
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

    fun updateRooms() {
        if (LocationUtils.inBoss) return
        if (DungeonListener.dungeonEnded) return
        if (DungeonListener.thePlayer?.isDead == true) return
        val mapData = DungeonInfo.mapData ?: return
        HotbarMapColorParser.updateMap(mapData)

        for (x in 0 .. 10) {
            for (z in 0 .. 10) {
                val idx = z * 11 + x
                val room = DungeonInfo.dungeonList[idx]
                val mapTile = HotbarMapColorParser.getTile(x, z)

                if (room is Unknown) {
                    DungeonInfo.dungeonList[idx] = mapTile
                    DungeonPathFinder.clearCache()
                    if (mapTile is Room) {
                        val connected = HotbarMapColorParser.getConnected(x, z)
                        connected.firstOrNull { it.data.name != "Unknown" }?.let {
                            mapTile.addToUnique(z, x, it.data.name)
                        }
                    }
                    continue
                }

                if (mapTile.state.ordinal < room.state.ordinal || mapTile is Room && room is Room && mapTile.data.type == RoomType.PUZZLE) {
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
                    if (mapTile is Door && mapTile.type == DoorType.WITHER) room.opened = false
                    else if (! room.opened) {
                        if (WorldUtils.isChunkLoaded(room.x, room.z)) {
                            if (LegacyRegistry.getLegacyId(WorldUtils.getStateAt(room.x, 69, room.z)).equalsOneOf(0, 166)) room.opened = true
                        }
                        else if (mapTile is Door && mapTile.state == RoomState.DISCOVERED) {
                            if (room.type == DoorType.BLOOD) {
                                val bloodRoom = DungeonInfo.dungeonList.filterIsInstance<Room>().find { it.data.type == RoomType.BLOOD }
                                if (bloodRoom != null && bloodRoom.state != RoomState.UNOPENED) room.opened = true
                            }
                            else room.opened = true
                        }
                    }
                }
            }
        }
    }
}