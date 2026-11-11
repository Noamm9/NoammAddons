package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.mixin.IMapState
import com.github.noamm9.utils.*
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonListener.dungeonTeammatesNoSelf
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.RenderHelper.renderVec
import kotlinx.coroutines.*
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.saveddata.maps.*
import java.util.concurrent.*

object MapUpdater: ISelfInit {
    private val playerHeadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val renderPositions = ConcurrentHashMap<String, Triple<Float, Float, Float>>()
    private val playerJobs = ConcurrentHashMap<String, Job>()

    override fun init() {
        EventBus.register<WorldChangeEvent> {
            playerJobs.forEach { it.value.cancel() }
            playerJobs.clear()
            renderPositions.clear()
        }

        EventBus.register<MainThreadPacketReceivedEvent.Post> {
            if (! LocationUtils.inDungeon) return@register
            val packet = event.packet as? ClientboundMapItemDataPacket ?: return@register
            val mapId = PlayerUtils.getHotbarSlot(8)?.get(DataComponents.MAP_ID) ?: packet.mapId
            val mapData = mc.level?.getMapData(mapId) ?: return@register

            MapUtils.calibrated = MapUtils.calibrateMap(mapData)
            if (MapUtils.calibrated) {
                DungeonListener.dungeonStarted = true
                updateRooms(mapData)
                updatePlayers(mapData)
            }
        }
    }

    fun updatePlayers(mapData: MapItemSavedData) {
        val mapData = mapData as? IMapState ?: return
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
            teammate.mapX = decoration.mapX.toFloat()
            teammate.mapZ = decoration.mapZ.toFloat()
            teammate.yaw = decoration.yaw
            smoothUpdatePlayer(teammate)
        }
    }

    private fun smoothUpdatePlayer(player: DungeonPlayer) {
        if (renderPositions.putIfAbsent(player.name, Triple(player.mapX, player.mapZ, player.yaw)) == null) return
        val targetX = player.mapX
        val targetZ = player.mapZ
        val targetYaw = player.yaw

        playerHeadScope.launch {
            playerJobs.put(player.name, coroutineContext.job)?.cancelAndJoin()

            val (startX, startZ, startYaw) = renderPositions[player.name] ?: return@launch
            if (startX == targetX && startZ == targetZ && startYaw == targetYaw) return@launch

            val animationDuration = 350L
            val startTime = System.currentTimeMillis()
            var progress = 0f

            while (progress < 1f && isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                progress = (elapsedTime.toFloat() / animationDuration).coerceAtMost(1f)

                renderPositions[player.name] = Triple(
                    MathUtils.lerp(startX, targetX, progress).toFloat(),
                    MathUtils.lerp(startZ, targetZ, progress).toFloat(),
                    MathUtils.interpolateYaw(startYaw, targetYaw, progress)
                )

                delay(10)
            }
        }
    }

    fun DungeonPlayer.animatedPos(): Triple<Float, Float, Float> {
        val entity = entity
        if (entity != null && entity.isAlive) {
            val (mx, mz) = MapUtils.coordsToMap(entity.renderVec)
            return Triple(mx, mz, entity.yRot).also { renderPositions[name] = it }
        }

        if (! MapConfig.animatePlayerHeads.value) return Triple(mapX, mapZ, yaw)
        return renderPositions[name] ?: Triple(mapX, mapZ, yaw)
    }

    fun updateRooms(mapData: MapItemSavedData) {
        if (LocationUtils.inBoss) return
        if (DungeonListener.dungeonEnded) return
        if (DungeonListener.thePlayer?.isDead == true) return
        HotbarMapScanner.updateMap(mapData)

        for (x in 0 .. 10) for (z in 0 .. 10) {
            val idx = z * 11 + x
            val room = DungeonScanner.dungeonList[idx]
            val mapTile = HotbarMapScanner.getTile(x, z)

            if (room is Unknown) {
                DungeonScanner.setTile(idx, mapTile)
                if (mapTile is RoomTile) {
                    val connected = HotbarMapScanner.getConnected(x, z)
                    connected.firstOrNull { it.data.name != "Unknown" }?.let {
                        mapTile.addToUnique(z, x, it.data.name)
                    }
                }
                continue
            }

            if (mapTile.state.ordinal < room.state.ordinal || mapTile is RoomTile && room is RoomTile && mapTile.data.type == RoomType.PUZZLE) {
                room.state = mapTile.state
            }

            if (mapTile is RoomTile && room is RoomTile && mapTile.data.type != room.data.type) {
                if (room.data.name == mapTile.data.name) room.data = mapTile.data
            }

            if (mapTile is DoorTile && room is DoorTile) {
                if (mapTile.type == DoorType.WITHER && room.type != DoorType.WITHER) {
                    room.type = mapTile.type
                }
            }

            if (room is DoorTile && room.type.equalsOneOf(DoorType.ENTRANCE, DoorType.WITHER, DoorType.BLOOD)) {
                if (mapTile is DoorTile && mapTile.type == DoorType.WITHER) room.opened = false
                else if (! room.opened) {
                    if (WorldUtils.isChunkLoaded(room.x, room.z)) {
                        val state = WorldUtils.getStateAt(room.x, 69, room.z)
                        if (state.isAir || state.`is`(Blocks.BARRIER)) room.opened = true
                    }
                    else if (mapTile is DoorTile && mapTile.state == RoomState.DISCOVERED) {
                        if (room.type == DoorType.BLOOD) {
                            val bloodRoomTile = DungeonScanner.dungeonList.filterIsInstance<RoomTile>().find { it.data.type == RoomType.BLOOD }
                            if (bloodRoomTile != null && bloodRoomTile.state != RoomState.UNOPENED) room.opened = true
                        }
                        else room.opened = true
                    }
                }
            }
        }

        SpecialColumn.update()
    }

    private val MapDecoration.mapX get() = (this.x + 128) shr 1
    private val MapDecoration.mapZ get() = (this.y + 128) shr 1
    private val MapDecoration.yaw get() = this.rot * 22.5f
}