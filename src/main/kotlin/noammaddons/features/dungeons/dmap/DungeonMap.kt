package noammaddons.features.dungeons.dmap

import gg.essential.elementa.utils.withAlpha
import net.minecraft.item.ItemMap
import net.minecraft.network.play.server.S34PacketMaps
import net.minecraft.world.storage.MapData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.dungeons.dmap.core.DungeonMapElement
import noammaddons.features.dungeons.dmap.core.map.*
import noammaddons.features.dungeons.dmap.handlers.*
import noammaddons.features.dungeons.dmap.utils.MapUtils
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon

object DungeonMap: Feature() {
    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! DungeonMapConfig.mapEnabled || ! inDungeon || mc.thePlayer == null || mc.theWorld == null) return
        if (! DungeonMapConfig.dungeonMapCheater && ! dungeonStarted) return
        if (DungeonMapConfig.mapHideInBoss && inBoss) return

        try {
            DungeonMapElement.draw()
        }
        catch (e: Exception) {
            e.printStackTrace()
            modMessage("Error while drawing map: ${e.message}")
        }
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! inDungeon || mc.thePlayer == null) return

        if (dungeonStarted) {
            if (! MapUtils.calibrated) {
                if (DungeonInfo.dungeonMap == null) {
                    DungeonInfo.dungeonMap = MapUtils.getMapData()
                }
                MapUtils.calibrated = MapUtils.calibrateMap()
            }

            (DungeonInfo.dungeonMap ?: DungeonInfo.guessMapData)?.let {
                MapUpdater.updateRooms(it)
                MapUpdater.updatePlayers(it)
            }
        }

        if (DungeonScanner.shouldScan || config.DevMode) {
            DungeonScanner.scan()
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        DungeonInfo.reset()
        DungeonScanner.hasScanned = false
        MapUtils.calibrated = false
        MapUtils.startCorner = Pair(5, 5)
        MapUtils.mapRoomSize = 16
        MapUtils.coordMultiplier = 0.625
        MapUpdater.playerPositions.clear()
        MapUpdater.playerJobs.clear()
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorld) {
        if (! inDungeon || ! DungeonMapConfig.boxWitherDoors) return
        val witherDoors = DungeonInfo.dungeonList
            .filterIsInstance<Door>()
            .filter { it.type != DoorType.NORMAL && ! it.opened }
            .filterNot { (dungeonStarted || ! DungeonMapConfig.dungeonMapCheater) && it.state == RoomState.UNDISCOVERED }

        witherDoors.forEach {
            val color = if (DungeonInfo.keys > 0) DungeonMapConfig.witherDoorKeyColor
            else DungeonMapConfig.witherDoorNoKeyColor

            RenderUtils.drawBox(
                it.x - 1, 69.0, it.z - 1,
                width = 3, height = 4,
                color = color.withAlpha(DungeonMapConfig.witherDoorFill),
                outline = true, fill = true, phase = true,
                LineThickness = DungeonMapConfig.witherDoorOutlineWidth
            )
        }
    }

    @SubscribeEvent
    fun onPuzzleReset(event: DungeonEvent.PuzzleEvent.Reset) {
        DungeonInfo.uniqueRooms.find {
            it.mainRoom.data.type == RoomType.PUZZLE && Puzzle.fromName(it.name)?.tabName == event.pazzle
        }?.run { mainRoom.state = RoomState.DISCOVERED }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! inDungeon) return
        if (event.packet !is S34PacketMaps) return
        if (DungeonInfo.dungeonMap != null) return
        if (mc.theWorld == null) return
        val id = event.packet.mapId
        if (id and 1000 != 0) return

        val guess = mc.theWorld.mapStorage.loadData(MapData::class.java, "map_${id}") as MapData? ?: return
        if (guess.mapDecorations.any { it.value.func_176110_a() == 1.toByte() }) {
            DungeonInfo.guessMapData = guess
        }

        if (MapUtils.calibrated) {
            ItemMap.loadMapData(id, mc.theWorld)?.let { mapData ->
                MapUpdater.updateRooms(mapData)
                MapUpdater.updatePlayers(mapData)
            }
        }
    }

    @SubscribeEvent
    fun onMessage(event: MessageSentEvent) {
        var text = event.message.removeFormatting().lowercase()
        val commandName = "/dmap"
        if (! text.startsWith(commandName)) return
        text = text.replace(commandName, "")
        event.isCanceled = true
        when (text) {
            "", " " -> config.openDungeonMapConfig()
            " setexplored" -> DungeonInfo.dungeonList.forEach { it.state = RoomState.DISCOVERED }
            " debug" -> {
                listOf(
                    "inBoss: ${LocationUtils.inBoss}",
                    "dungeonStarted: ${DungeonUtils.dungeonStarted}",
                    "DungeonInfo.dungeonMap: ${DungeonInfo.dungeonMap != null}",
                    "DungeonInfo.guessMapData: ${DungeonInfo.guessMapData != null}",
                    "DungeonInfo.dungeonList: ${DungeonInfo.dungeonList.size}",
                    "DungeonInfo.uniqueRooms: ${DungeonInfo.uniqueRooms.size}",
                    "DungeonInfo.playerIcons: ${DungeonInfo.playerIcons.size}",
                    "DungeonInfo.roomCount: ${DungeonInfo.roomCount}",
                    "DungeonInfo.puzzles: ${DungeonInfo.puzzles.size}",
                    "DungeonInfo.trapType: ${DungeonInfo.trapType}",
                    "DungeonInfo.witherDoors: ${DungeonInfo.witherDoors}",
                    "DungeonInfo.keys: ${DungeonInfo.keys}",
                    "MapUtils.calibrated: ${MapUtils.calibrated}",
                    "MapUtils.startCorner: ${MapUtils.startCorner}",
                    "MapUtils.mapRoomSize: ${MapUtils.mapRoomSize}",
                    "MapUtils.coordMultiplier: ${MapUtils.coordMultiplier}",
                    "MapUpdater.playerPositions: ${MapUpdater.playerPositions.size}",
                    "MapUpdater.playerJobs: ${MapUpdater.playerJobs.size}",
                    "DungeonScanner.hasScanned: ${DungeonScanner.hasScanned}",
                    "DungeonScanner.shouldScan: ${DungeonScanner.shouldScan}",
                    "MapUpdater.playerPositions: ${MapUpdater.playerPositions.size}",
                    "MapUpdater.playerJobs: ${MapUpdater.playerJobs.size}",
                    "DungeonInfo.dungeonList: ${DungeonInfo.dungeonList.size}",
                    "DungeonInfo.uniqueRooms: ${DungeonInfo.uniqueRooms.size}",
                    "DungeonInfo.playerIcons: ${DungeonInfo.playerIcons.size}",
                    "DungeonInfo.roomCount: ${DungeonInfo.roomCount}",
                    "DungeonInfo.puzzles: ${DungeonInfo.puzzles.size}",
                    "DungeonInfo.trapType: ${DungeonInfo.trapType}",
                    "DungeonInfo.witherDoors: ${DungeonInfo.witherDoors}",
                    "DungeonInfo.cryptCount: ${DungeonInfo.cryptCount}",
                    "DungeonInfo.keys: ${DungeonInfo.keys}",
                    "MapUtils.calibrated: ${MapUtils.calibrated}",
                    "MapUtils.startCorner: ${MapUtils.startCorner}",
                    "MapUtils.mapRoomSize: ${MapUtils.mapRoomSize}",
                    "MapUtils.coordMultiplier: ${MapUtils.coordMultiplier}",
                    "MapUpdater.playerPositions: ${MapUpdater.playerPositions.size}",
                    "MapUpdater.playerJobs: ${MapUpdater.playerJobs.size}",
                    "DungeonScanner.hasScanned: ${DungeonScanner.hasScanned}",
                    "DungeonScanner.shouldScan: ${DungeonScanner.shouldScan}",
                    "MapUpdater.playerPositions: ${MapUpdater.playerPositions.size}",
                    "MapUpdater.playerJobs: ${MapUpdater.playerJobs.size}",
                    "DungeonInfo.dungeonList: ${DungeonInfo.dungeonList.size}",
                    "DungeonInfo.uniqueRooms: ${DungeonInfo.uniqueRooms.size}",
                    "DungeonInfo.playerIcons: ${DungeonInfo.playerIcons.size}",
                    "DungeonInfo.roomCount: ${DungeonInfo.roomCount}",
                    "DungeonInfo.puzzles: ${DungeonInfo.puzzles.size}",
                    "DungeonInfo.trapType: ${DungeonInfo.trapType}",
                    "DungeonInfo.witherDoors: ${DungeonInfo.witherDoors}",
                    "DungeonInfo.secretCount: ${DungeonInfo.secretCount}",
                    "DungeonInfo.keys: ${DungeonInfo.keys}",
                    "MapUtils.calibrated: ${MapUtils.calibrated}",
                    "MapUtils.startCorner: ${MapUtils.startCorner}",
                    "MapUtils.mapRoomSize: ${MapUtils.mapRoomSize}",
                    "MapUtils.coordMultiplier: ${MapUtils.coordMultiplier}",
                    "MapUpdater.playerPositions: ${MapUpdater.playerPositions.size}",
                    "MapUpdater.playerJobs: ${MapUpdater.playerJobs.size}",
                    "DungeonScanner.hasScanned: ${DungeonScanner.hasScanned}",
                    "DungeonScanner.shouldScan: ${DungeonScanner.shouldScan}",
                    "MapUpdater.playerPositions: ${MapUpdater.playerPositions.size}",
                    "MapUpdater.playerJobs: ${MapUpdater.playerJobs.size}",
                    "DungeonInfo.dungeonList: ${DungeonInfo.dungeonList.size}",
                    "DungeonInfo.uniqueRooms: ${DungeonInfo.uniqueRooms.size}",
                    "DungeonInfo.playerIcons: ${DungeonInfo.playerIcons.size}",
                    "DungeonInfo.roomCount: ${DungeonInfo.roomCount}",
                    "DungeonInfo.puzzles: ${DungeonInfo.puzzles.size}",
                    "DungeonInfo.trapType: ${DungeonInfo.trapType}",
                    "DungeonInfo.witherDoors: ${DungeonInfo.witherDoors}"
                ).toSet().forEach {
                    modMessage(it)
                }
            }
        }
    }
}