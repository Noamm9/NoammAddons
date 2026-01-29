package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.ui.clickgui.componnents.impl.CategorySetting
import com.github.noamm9.ui.clickgui.componnents.impl.SeparatorSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Door
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.handlers.ClearInfoUpdater
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.handlers.MapUpdater
import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket

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
            DungeonScanner.hasScanned = false
            MapUtils.reset()
            MapUpdater.onPlayerDeath()
            ScoreCalculation.reset()
            DungeonInfo.mimicRoom = null
        }

        register<RenderWorldEvent> {
            if (! enabled || ! LocationUtils.inDungeon || LocationUtils.inBoss || ! MapConfig.boxWitherDoors.value) return@register

            val shouldHideUndiscovered = ! MapConfig.dungeonMapCheater.value || DungeonListener.dungeonStarted
            val color = (if (DungeonListener.doorKeys > 0) MapConfig.witherDoorKeyColor.value else MapConfig.witherDoorNoKeyColor.value).withAlpha((MapConfig.witherDoorFill.value * 2.55).toInt())

            for (tile in DungeonInfo.dungeonList) {
                if (tile !is Door) continue
                if (tile.opened || tile.type == DoorType.ENTRANCE || tile.type == DoorType.NORMAL) continue
                if (shouldHideUndiscovered && tile.state == RoomState.UNDISCOVERED) continue

                Render3D.renderBox(
                    event.ctx,
                    tile.x + 0.5, 69.0, tile.z + 0.5,
                    3, 4, color,
                    outline = true,
                    fill = true,
                    phase = true
                )
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