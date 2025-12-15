@file:Suppress("UNUSED_PARAMETER")

package noammaddons.features.impl.dungeons.dmap

import gg.essential.api.EssentialAPI
import gg.essential.elementa.utils.withAlpha
import net.minecraft.item.ItemMap
import net.minecraft.network.play.server.S34PacketMaps
import net.minecraft.world.storage.MapData
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.dungeons.MimicDetector
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapElement
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.features.impl.dungeons.dmap.handlers.*
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.utils.*
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon

@AlwaysActive
object DungeonMap: Feature() {
    override fun init() = addSettings(*DungeonMapConfig.setup())

    val debug get() = EssentialAPI.getMinecraftUtil().isDevelopment() || DevOptions.devMode || DevOptions.enabled

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderOverlayNoCaching(event: RenderOverlayNoCaching) {
        if (! enabled) return
        if (mc.gameSettings.keyBindPlayerList.isKeyDown) return
        if (! DungeonMapConfig.mapEnabled.value || ! inDungeon) return
        if (! DungeonMapConfig.dungeonMapCheater.value && ! DungeonUtils.dungeonStarted) return
        if (DungeonMapConfig.mapHideInBoss.value && inBoss) return

        DungeonMapElement.draw()
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderOverlay(event: RenderOverlay) {
        if (! enabled) return
        if (! mc.gameSettings.keyBindPlayerList.isKeyDown) return
        if (! DungeonMapConfig.mapEnabled.value || ! inDungeon) return
        if (! DungeonMapConfig.dungeonMapCheater.value && ! DungeonUtils.dungeonStarted) return
        if (DungeonMapConfig.mapHideInBoss.value && inBoss) return

        DungeonMapElement.draw()
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! inDungeon || mc.thePlayer == null) return

        if (DungeonScanner.shouldScan || DevOptions.devMode) {
            DungeonScanner.scan()
        }

        if (MimicDetector.mimicRoom == null && MimicDetector.shouldScanMimic) {
            MimicDetector.findMimicRoom()?.let {
                MimicDetector.mimicRoom = it
                it.hasMimic = true
            }
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        DungeonInfo.reset()
        DungeonScanner.hasScanned = false
        MapUtils.reset()
        MapUpdater.playerJobs.clear()
        ScoreCalculation.reset()
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorld) {
        if (! inDungeon || inBoss || ! DungeonMapConfig.boxWitherDoors.value) return

        val shouldHideUndiscovered = ! DungeonMapConfig.dungeonMapCheater.value || (DungeonUtils.dungeonStarted && ScanUtils.getEntityRoom(mc.thePlayer)?.data?.type != RoomType.FAIRY)
        val color = (if (DungeonInfo.keys > 0) DungeonMapConfig.witherDoorKeyColor.value else DungeonMapConfig.witherDoorNoKeyColor.value).withAlpha(DungeonMapConfig.witherDoorFill.value)

        for (tile in DungeonInfo.dungeonList) {
            if (tile !is Door) continue
            if (tile.opened || tile.type == DoorType.ENTRANCE || tile.type == DoorType.NORMAL) continue
            if (shouldHideUndiscovered && tile.state == RoomState.UNDISCOVERED) continue

            RenderUtils.drawBox(
                tile.x - 1.0, 69.0, tile.z - 1.0,
                color, outline = true, fill = true,
                width = 3.0, height = 4.0, phase = true
            )
        }
    }

    @SubscribeEvent
    fun onPacketRecivedRecived(event: MainThreadPacketRecivedEvent.Post) {
        if (! inDungeon) return
        ScoreCalculation.onPacket(event.packet)

        val packet = event.packet as? S34PacketMaps ?: return
        val mapId = packet.mapId.takeIf { it in 1000 .. 1400 } ?: return

        if (MapUtils.mapId == null) {
            val hotbarId = mc.thePlayer.inventory.getStackInSlot(8)?.takeIf { it.item is ItemMap }?.metadata
            DungeonInfo.mapData = mc.theWorld.loadItemData(MapData::class.java, "map_${hotbarId ?: mapId}") as? MapData ?: return
            if (mapId == hotbarId) MapUtils.mapId = mapId
        }

        if (! MapUtils.calibrated) MapUtils.calibrated = MapUtils.calibrateMap()

        if (MapUtils.calibrated) {
            MapUpdater.updateRooms()
            MapUpdater.updatePlayers()
        }
    }

    @SubscribeEvent
    fun onPuzzleReset(event: DungeonEvent.PuzzleEvent.Reset) {
        event.pazzle.room?.mainRoom?.state = RoomState.DISCOVERED
    }

    @SubscribeEvent
    fun onRoomStateChangeEvent(event: DungeonEvent.RoomEvent.onStateChange) {
        ClearInfoUpdater.checkSplits(event.room.data, event.oldState, event.newState, event.roomPlayers)
        if (event.newState == RoomState.GREEN) event.room.foundSecrets = event.room.data.secrets
    }

    @SubscribeEvent
    fun onPlayerDeathEvent(event: DungeonEvent.PlayerDeathEvent) {
        ClearInfoUpdater.updateDeaths(event.name, event.reason)
        MapUpdater.onPlayerDeath()
        ScoreCalculation.deathCount ++
    }

    @SubscribeEvent
    fun onRunStartEvent(event: DungeonEvent.RunStatedEvent) {
        ClearInfoUpdater.initStartSecrets()
    }

    @SubscribeEvent
    fun onRunEndEvent(event: DungeonEvent.RunEndedEvent) {
        ClearInfoUpdater.sendClearInfoMessage()
    }
}