@file:Suppress("UNUSED_PARAMETER")

package noammaddons.features.dungeons.dmap

import gg.essential.api.EssentialAPI
import gg.essential.elementa.utils.withAlpha
import kotlinx.coroutines.*
import net.minecraft.event.HoverEvent
import net.minecraft.item.ItemMap
import net.minecraft.network.play.server.S34PacketMaps
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.world.storage.MapData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.dungeons.dmap.core.*
import noammaddons.features.dungeons.dmap.core.map.*
import noammaddons.features.dungeons.dmap.handlers.*
import noammaddons.features.dungeons.dmap.utils.MapUtils
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.Utils.equalsOneOf

object DungeonMap: Feature() {
    private val debug get() = EssentialAPI.getMinecraftUtil().isDevelopment()

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! DungeonMapConfig.mapEnabled || ! inDungeon) return
        if (! DungeonMapConfig.dungeonMapCheater && ! DungeonUtils.dungeonStarted) return
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

        if (DungeonUtils.dungeonStarted) {
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
        MapUpdater.playerJobs.clear()
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorld) {
        if (! inDungeon || ! DungeonMapConfig.boxWitherDoors) return
        DungeonInfo.dungeonList.filterIsInstance<Door>()
            .filter { ! it.type.equalsOneOf(DoorType.ENTRANCE, DoorType.NORMAL) && ! it.opened }
            .filterNot { (DungeonUtils.dungeonStarted || ! DungeonMapConfig.dungeonMapCheater) && it.state == RoomState.UNDISCOVERED }
            .forEach {
                val color = if (DungeonInfo.keys > 0) DungeonMapConfig.witherDoorKeyColor
                else DungeonMapConfig.witherDoorNoKeyColor

                RenderUtils.drawBox(
                    it.x - 1, 69.0, it.z - 1,
                    width = 3, height = 4,
                    color = color.withAlpha(DungeonMapConfig.witherDoorFill),
                    outline = true, fill = true, phase = true,
                    lineWidth = DungeonMapConfig.witherDoorOutlineWidth
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
        }
    }

    @SubscribeEvent
    fun onRoomStateChangeEvent(event: DungeonEvent.RoomEvent.onStateChange) {
        if (event.roomPlayers.isEmpty()) return
        if (event.room.type.equalsOneOf(RoomType.FAIRY, RoomType.ENTRANCE)) return
        if (! event.oldState.equalsOneOf(RoomState.UNDISCOVERED, RoomState.DISCOVERED, RoomState.UNOPENED)) return
        if (! event.newState.equalsOneOf(RoomState.CLEARED, RoomState.GREEN)) return

        if (event.roomPlayers.size == 1) {
            ClearInfo.get(event.roomPlayers[0].name).clearedRooms.first.add(event.room.name)
            if (debug) modMessage("${event.roomPlayers[0].name} cleared ${event.room.name}")
        }
        else event.roomPlayers.forEach {
            ClearInfo.get(it.name).clearedRooms.second.add(event.room.name)
            if (debug) modMessage("${it.name} stacked cleard ${event.room.name}")
        }
    }

    @SubscribeEvent
    fun onPlayerDeathEvent(event: DungeonEvent.PlayerDeathEvent) {
        ClearInfo.get(event.name).deaths += event.reason
        if (debug) modMessage("${event.name} died: ${event.reason}")
        if (TablistListener.deathCount == 0) {
            DungeonInfo.firstDeathHadSpirit = ProfileUtils.getSpiritPet(event.name)
        }
    }

    @SubscribeEvent
    fun onRunStartEvent(event: DungeonEvent.RunStatedEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            DungeonUtils.runPlayersNames.keys.toList().forEach { name ->
                val secrets = ProfileUtils.getSecrets(name)
                ClearInfo.get(name).secretsBeforeRun = secrets
                if (debug) modMessage("$name has $secrets secrets")
            }
        }
    }

    @SubscribeEvent
    fun onRunEndEvent(event: DungeonEvent.RunEndedEvent) {
        val msgList = mutableListOf<ChatComponentText>()

        CoroutineScope(Dispatchers.IO).launch {
            DungeonUtils.dungeonTeammates.toList().forEach { teammate ->
                val secretsAfterRun = ProfileUtils.getSecrets(teammate.name).also {
                    if (debug) modMessage("${teammate.name} has $it secrets after the run")
                }
                val playerFormatted = "${DungeonUtils.Classes.getColorCode(teammate.clazz)}${teammate.name}"
                val foundSecrets = secretsAfterRun - teammate.clearInfo.secretsBeforeRun
                val base = createComponent("$CHAT_PREFIX $playerFormatted&f:&r ")
                val separator = createComponent(" &f|&r ")
                val solo = teammate.clearInfo.clearedRooms.first
                val stacked = teammate.clearInfo.clearedRooms.second

                val roomComponent = if (solo.size + stacked.size == 0) createComponent("&e0 Rooms&r")
                else {
                    val roomRange = if (stacked.isEmpty()) "${solo.size}" else "${solo.size}-${solo.size + stacked.size}"
                    val tooltip = buildString {
                        append(solo.joinToString("\n") { "$it &b(Solo)&r" })
                        if (solo.isNotEmpty() && stacked.isNotEmpty()) append("\n")
                        append(stacked.joinToString("\n") { "$it &d(stack)&r" })
                    }

                    createComponent("&e$roomRange Rooms&r", tooltip)
                }

                val secretsComponent = createComponent("&b$foundSecrets Secrets&r")

                val deathsComponent = if (teammate.clearInfo.deaths.isEmpty()) null
                else createComponent("&c${teammate.clearInfo.deaths.size} Deaths", teammate.clearInfo.deaths.joinToString("\n"))

                listOfNotNull(
                    roomComponent, separator,
                    secretsComponent,
                    if (deathsComponent != null) separator
                    else null, deathsComponent
                ).forEach(base::appendSibling)

                msgList.add(base)
            }

            msgList.forEach(mc.thePlayer::addChatMessage)
        }
    }

    private fun createComponent(text: String, hoverText: String? = null) = ChatComponentText(text.addColor()).apply {
        if (hoverText != null) {
            chatStyle = ChatStyle().apply {
                chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText.addColor()))
            }
        }
    }
}