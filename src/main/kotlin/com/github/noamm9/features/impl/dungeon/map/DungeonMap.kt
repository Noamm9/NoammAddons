package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.SeparatorSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Door
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.handlers.*
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.phys.Vec3

@AlwaysActive
object DungeonMap: Feature() {

    override fun init() {
        MapConfig.setup().forEach {
            it.headerName?.let { name ->
                if (configSettings.isNotEmpty()) {
                    configSettings.add(SeparatorSetting())
                }
                configSettings.add(CategorySetting(name))
            }

            configSettings.add(it)
        }

        hudElements.add(MapRenderer)

        register<TickEvent.Start> {
            if (! LocationUtils.inDungeon || mc.player == null) return@register

            if (DungeonScanner.shouldScan && WorldUtils.isChunkLoaded(mc.player !!.x.toInt(), mc.player !!.z.toInt())) {
                DungeonScanner.scan()
            }

            if (DungeonInfo.mimicRoom == null && (LocationUtils.dungeonFloorNumber ?: 0) > 5 && ! LocationUtils.inBoss && ! ScoreCalculation.mimicKilled) {
                DungeonScanner.findMimicRoom()?.let {
                    DungeonInfo.mimicRoom = it
                    it.hasMimic = true
                }
            }
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! LocationUtils.inDungeon) return@register
            ScoreCalculation.onPacket(event.packet)
            val packet = event.packet as? ClientboundMapItemDataPacket ?: return@register
            val mapId = PlayerUtils.getHotbarSlot(8)?.get(DataComponents.MAP_ID) ?: packet.mapId

            DungeonInfo.mapData = mc.level?.getMapData(mapId)

            if (! MapUtils.calibrated) MapUtils.calibrated = MapUtils.calibrateMap()

            if (MapUtils.calibrated) {
                MapUpdater.updateRooms()
                MapUpdater.updatePlayers()
            }
        }

        register<WorldChangeEvent> {
            DungeonInfo.reset()
            DungeonPathFinder.clearCache()
            DungeonScanner.hasScanned = false
            MapUtils.reset()
            MapUpdater.onPlayerDeath()
            ScoreCalculation.reset()
            DungeonInfo.mimicRoom = null
        }

        register<RenderWorldEvent> {
            val boxDoors = MapConfig.boxWitherDoors.value
            val traceDoors = MapConfig.traceWitherDoors.value
            if (!enabled || !LocationUtils.inDungeon || LocationUtils.inBoss || (!boxDoors && !traceDoors)) return@register

            val cheaterMap = MapConfig.dungeonMapCheater.value
            val bloodRush = DungeonPathFinder.getBloodRush()
            if (bloodRush.size < 2) return@register

            val currentIndex = listOfNotNull(ScanUtils.currentRoom, ScanUtils.lastKnownRoom)
                .map { bloodRush.indexOf(it) }
                .filter { it >= 0 }
                .maxOrNull() ?: 0
            val futureDoors = (currentIndex ..< bloodRush.lastIndex).mapNotNull { index ->
                DungeonInfo.dungeonList.firstNotNullOfOrNull { tile ->
                    val door = tile as? Door ?: return@firstNotNullOfOrNull null
                    if ((door.type != DoorType.WITHER && door.type != DoorType.BLOOD) || door.opened) return@firstNotNullOfOrNull null
                    if (!cheaterMap && door.state == RoomState.UNDISCOVERED && !DungeonPathFinder.isFairy(door)) return@firstNotNullOfOrNull null

                    val rooms = DungeonPathFinder.getConnectingDoorRooms(door.arrayPos.first, door.arrayPos.second)
                        .mapNotNull { it.uniqueRoom }

                    door.takeIf { rooms.size == 2 && bloodRush[index] in rooms && bloodRush[index + 1] in rooms }
                }
            }
            val nextDoor = futureDoors.firstOrNull()

            if (boxDoors) {
                val doorsToBox = if (cheaterMap && MapConfig.boxAllWitherDoors.value) {
                    DungeonInfo.dungeonList.filterIsInstance<Door>()
                        .filter { (it.type == DoorType.WITHER || it.type == DoorType.BLOOD) && ! it.opened }
                }
                else listOfNotNull(nextDoor)

                for (tile in doorsToBox) {
                    val doorColor = if (tile == nextDoor && DungeonListener.doorKeys > 0) MapConfig.witherDoorKeyColor.value else MapConfig.witherDoorNoKeyColor.value
                    Render3D.renderBox(
                        event.ctx,
                        tile.x + 0.5, 69.0, tile.z + 0.5,
                        3, 4, doorColor.withAlpha((MapConfig.witherDoorFill.value * 2.55).toInt()),
                        outline = true,
                        fill = true,
                        phase = true
                    )
                }
            }

            if (traceDoors && nextDoor != null) {
                val tracerColor = if (DungeonListener.doorKeys > 0) MapConfig.witherDoorKeyColor.value else MapConfig.witherDoorNoKeyColor.value
                Render3D.renderTracer(event.ctx, Vec3(nextDoor.x + 0.5, 71.0, nextDoor.z + 0.5), tracerColor)
            }
        }

        register<DungeonEvent.RoomEvent.onStateChange> {
            ClearInfoUpdater.checkSplits(event.room.data, event.oldState, event.newState, event.roomPlayers)
            if (event.newState == RoomState.GREEN) event.room.foundSecrets = event.room.data.secrets
        }

        register<DungeonEvent.PlayerDeathEvent> {
            ClearInfoUpdater.updateDeaths(event.name, event.reason)
            MapUpdater.onPlayerDeath()
            ScoreCalculation.deathCount ++
        }

        register<DungeonEvent.RunStatedEvent> {
            ClearInfoUpdater.initStartSecrets()
        }

        register<DungeonEvent.RunEndedEvent> {
            ClearInfoUpdater.sendClearInfoMessage()
        }
    }
}
