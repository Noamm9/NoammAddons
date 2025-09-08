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
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapElement
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.features.impl.dungeons.dmap.handlers.*
import noammaddons.features.impl.dungeons.dmap.handlers.ScoreCalculation.deathCount
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.remove

@AlwaysActive
object DungeonMap: Feature(toggled = false) {
    override fun init() = addSettings(*DungeonMapConfig.setup())

    val debug get() = EssentialAPI.getMinecraftUtil().isDevelopment() || DevOptions.devMode || DevOptions.enabled

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderOverlay(event: RenderOverlayNoCaching) {
        if (debug) {
            val currentRoom = ScanUtils.currentRoom
            val rot = currentRoom?.rotation
            val h = currentRoom?.highestBlock
            RenderUtils.drawText(
                listOf(
                    "roofHight: $h",
                    "rotation: $rot",
                ).joinToString("\n"),
                300, 300,
            )
        }

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
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        DungeonInfo.reset()
        DungeonScanner.hasScanned = false
        MapUtils.calibrated = false
        MapUtils.startCorner = Pair(5, 5)
        MapUtils.mapRoomSize = 16
        MapUtils.coordMultiplier = 0.625
        MapUpdater.playerJobs.clear()
        ScoreCalculation.onWorldUnload()
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorld) {
        if (! inDungeon || ! DungeonMapConfig.boxWitherDoors.value || inBoss) return
        DungeonInfo.dungeonList.filterIsInstance<Door>()
            .filterNot { it.type.equalsOneOf(DoorType.ENTRANCE, DoorType.NORMAL) || it.opened }
            .filterNot { (DungeonUtils.dungeonStarted || ! DungeonMapConfig.dungeonMapCheater.value) && it.state == RoomState.UNDISCOVERED }
            .forEach {
                val color = if (DungeonInfo.keys > 0) DungeonMapConfig.witherDoorKeyColor.value
                else DungeonMapConfig.witherDoorNoKeyColor.value

                RenderUtils.drawBox(
                    it.x - 1, 69.0, it.z - 1,
                    width = 3, height = 4,
                    color = color.withAlpha(DungeonMapConfig.witherDoorFill.value),
                    outline = true, fill = true, phase = true,
                    lineWidth = DungeonMapConfig.witherDoorOutlineWidth.value
                )
            }
    }

    @SubscribeEvent
    fun onPuzzleReset(event: DungeonEvent.PuzzleEvent.Reset) {
        DungeonInfo.uniqueRooms.filter { it.mainRoom.data.type == RoomType.PUZZLE }.find {
            event.pazzle == Puzzle.fromName(it.name)?.tabName
        }?.run { mainRoom.state = RoomState.DISCOVERED }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! inDungeon) return
        ScoreCalculation.onPacket(event.packet)

        val packet = event.packet as? S34PacketMaps ?: return
        val mapData = mc.thePlayer?.inventory?.getStackInSlot(8)
            ?.takeIf { it.item is ItemMap || it.displayName.contains("Magical Map") }
            ?.let { (it.item as? ItemMap)?.getMapData(it, mc.theWorld) }
            ?: MapData("map_${packet.mapId}")

        mc.addScheduledTask {
            packet.setMapdataTo(mapData)
            DungeonInfo.mapData = mapData

            if (! MapUtils.calibrated) MapUtils.calibrated = MapUtils.calibrateMap()
            else {
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
        text = text.remove(commandName)
        event.isCanceled = true
        when (text) {
            " setexplored" -> DungeonInfo.dungeonList.forEach { it.state = RoomState.DISCOVERED }
        }
    }

    @SubscribeEvent
    fun onRoomStateChangeEvent(event: DungeonEvent.RoomEvent.onStateChange) {
        ClearInfoUpdater.checkSplits(event.room.data, event.oldState, event.newState, event.roomPlayers)
    }

    @SubscribeEvent
    fun onPlayerDeathEvent(event: DungeonEvent.PlayerDeathEvent) {
        ClearInfoUpdater.updateDeaths(event.name, event.reason)
        MapUpdater.onPlayerDeath()
        deathCount ++
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