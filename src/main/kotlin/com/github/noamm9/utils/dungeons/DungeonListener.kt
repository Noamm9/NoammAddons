package com.github.noamm9.utils.dungeons

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.*
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.enums.Blessing
import com.github.noamm9.utils.dungeons.enums.Classes
import com.github.noamm9.utils.dungeons.enums.Puzzle
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils.inDungeon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.network.protocol.game.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType


object DungeonListener {
    private val tablistRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$") // https://regex101.com/r/gv7bOe/1
    private val puzzleCountRegex = Regex("§b§lPuzzles: §f\\((?<count>\\d)\\)")
    private val puzzleRegex = Regex(" (.+): \\[[✦✔✖].+")
    private val deathRegex = Regex("^ ☠ (?:You were|(?<username>\\w+)) (?<reason>.+?)(?: and became a ghost)?\\.$") // https://regex101.com/r/Yc3HhV/4
    private val keyPickupRegex = Regex("^§e§lRIGHT CLICK §7on §7.+?§7 to open it\\. This key can only be used to open §a(?<num>\\d+)§7 door!$")
    private val witherDoorOpenedRegex = Regex("^(?:\\[.+?] )?(?<name>\\w+) opened a WITHER door!$")
    private val watcherMessageRegex = Regex("^\\[BOSS] The Watcher: .+$")
    private val runEndRegex = Regex("^\\s*(Master Mode)? ?(?:The)? Catacombs - (Floor (.{1,3})|Entrance)$") // https://regex101.com/r/W4UjWQ/3

    val runPlayersNames = mutableMapOf<String, ResourceLocation>()
    var dungeonTeammates = mutableListOf<DungeonPlayer>()
    var dungeonTeammatesNoSelf = listOf<DungeonPlayer>()
    var leapTeammates = listOf<DungeonPlayer>()
    var thePlayer: DungeonPlayer? = null

    var maxPuzzleCount = 0
    var puzzles = mutableListOf<Puzzle>()
    val dungeonStarted get() = dungeonTeammates.isNotEmpty()
    var dungeonStartTime: Long? = null
    var dungeonEnded = false

    var bloodOpenTime: Long? = null
    var watcherClearTime: Long? = null
    var watcherFinishSpawnTime: Long? = null
    var bossEntryTime: Long? = null
    var dungeonEndTime: Long? = null

    var lastDoorOpenner: DungeonPlayer? = null

    var currentTime = 0L
    var doorKeys = 0

    fun init() {
        register<MainThreadPacketReceivedEvent.Post>(EventPriority.HIGH) {
            if (! inDungeon) return@register

            when (val packet = event.packet) {
                is ClientboundPlayerInfoUpdatePacket -> {
                    val actions = packet.actions()
                    if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) || actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                        for (entry in packet.entries()) {
                            val text = entry.displayName()?.formattedText ?: continue

                            updateDungeonTeammates(text)
                            updatePuzzleCount(text)
                            updatePuzzles(text)
                        }
                    }
                }

                is ClientboundTabListPacket -> packet.footer.string?.let { footerText ->
                    Blessing.entries.forEach { blessing ->
                        blessing.regex.find(footerText)?.let {
                            blessing.current = it.groupValues[1].romanToDecimal()
                        }
                    }
                }

                is ClientboundContainerSetSlotPacket -> {
                    thePlayer?.isDead = PlayerUtils.getHotbarSlot(0)?.skyblockId == "HAUNT_ABILITY"
                }

                is ClientboundRemoveEntitiesPacket -> dungeonTeammates.forEach {
                    val id = it.entity?.id ?: return@forEach
                    if (id in packet.entityIds) it.entity = null
                }

                is ClientboundAddEntityPacket -> {
                    if (packet.type != EntityType.PLAYER) return@register
                    val entity = mc.level?.getEntity(packet.id) as? AbstractClientPlayer ?: return@register
                    dungeonTeammates.find { it.entity == null && it.name == entity.name.string }?.entity = entity
                }
            }
        }

        register<ChatMessageEvent>(EventPriority.HIGHEST) {
            if (! inDungeon) return@register
            val text = event.formattedText
            val unformatted = event.unformattedText

            when {
                unformatted.lowercase().contains("blaze done") -> {
                    puzzles.find { it.tabName == "Higher Or Lower" }?.let { puzzle ->
                        puzzle.state = RoomState.CLEARED
                        DungeonInfo.uniqueRooms.entries.find {
                            it.key.contains("Blaze")
                        }?.value?.mainRoom?.state = RoomState.CLEARED
                    }
                }

                unformatted.matches(runEndRegex) -> {
                    dungeonEnded = true
                    dungeonEndTime = currentTime
                    scope.launch { EventBus.post(DungeonEvent.RunEndedEvent) }
                }

                text == "§cThe §c§lBLOOD DOOR§c has been opened!" -> doorKeys --

                "§r§c ☠" in text && "reconnected" !in unformatted -> {
                    val match = deathRegex.find(unformatted) ?: return@register
                    val username = match.groups["username"]?.value?.takeUnless { it == "You" } ?: mc.user.name
                    val reason = match.groups["reason"]?.value ?: ""
                    scope.launch {
                        while (thePlayer == null) delay(1)
                        if (username == mc.user.name) thePlayer?.isDead = true
                        EventBus.post(DungeonEvent.PlayerDeathEvent(username, reason))
                    }
                }

                unformatted == "[BOSS] The Watcher: You have proven yourself. You may pass." -> {
                    DungeonInfo.uniqueRooms["Blood"]?.mainRoom?.state = RoomState.GREEN
                    watcherClearTime = currentTime
                }

                unformatted == "[BOSS] The Watcher: That will be enough for now." -> {
                    DungeonInfo.uniqueRooms["Blood"]?.mainRoom?.state = RoomState.CLEARED
                    watcherFinishSpawnTime = currentTime
                }

                watcherMessageRegex.matches(unformatted) && bloodOpenTime == null -> {
                    bloodOpenTime = currentTime
                }

                unformatted == "[NPC] Mort: Here, I found this map when I first entered the dungeon." -> scope.launch {
                    dungeonStartTime = currentTime
                    while (thePlayer == null) delay(1)
                    EventBus.post(DungeonEvent.RunStatedEvent)
                }

                else -> {
                    witherDoorOpenedRegex.find(unformatted)?.destructured?.let { (name) ->
                        lastDoorOpenner = dungeonTeammates.find { it.name == name }
                        doorKeys --
                        return@register
                    }

                    keyPickupRegex.find(text)?.destructured?.let { (num) ->
                        doorKeys += num.toInt()
                        return@register
                    }
                }
            }
        }

        register<TickEvent.Server>(EventPriority.HIGHEST) {
            if (! inDungeon) return@register
            currentTime ++
        }

        register<WorldChangeEvent>(EventPriority.HIGHEST) {
            runPlayersNames.clear()
            dungeonTeammates = mutableListOf()
            dungeonTeammatesNoSelf = mutableListOf()
            leapTeammates = mutableListOf()
            thePlayer = null
            maxPuzzleCount = 0
            puzzles.clear()
            dungeonStartTime = null
            dungeonEnded = false
            bloodOpenTime = null
            watcherClearTime = null
            watcherFinishSpawnTime = null
            bossEntryTime = null
            dungeonEndTime = null
            lastDoorOpenner = null
            currentTime = 0
            doorKeys = 0
            Blessing.reset()
        }

        register<DungeonEvent.RoomEvent.onStateChange> {
            if (lastDoorOpenner == null) return@register
            if (event.room.name != "Blood") return@register
            if (! event.newState.equalsOneOf(RoomState.DISCOVERED, RoomState.CLEARED, RoomState.GREEN)) return@register
            lastDoorOpenner = null
        }
    }

    private fun updateDungeonTeammates(tabName: String) {
        if (NoammAddons.debugFlags.contains("dev")) {
            listOf(
                DungeonPlayer("Noamm", Classes.Mage, 50, isDead = false),
                DungeonPlayer("Noamm9", Classes.Archer, 50, isDead = false),
                DungeonPlayer("NoammALT", Classes.Healer, 50, isDead = true),
                DungeonPlayer("NoamIsSad", Classes.Tank, 50, isDead = false),
            ).let { list ->
                dungeonTeammates.clear()
                dungeonTeammates.addAll(list)

                thePlayer = dungeonTeammates.find { it.name == mc.user.name }
                dungeonTeammatesNoSelf = dungeonTeammates.filterNot { it == thePlayer }
                leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }

                dungeonTeammates.onEach { teammate ->
                    teammate.entity = mc.level?.players()?.find { it.name.string == teammate.name } ?: teammate.entity
                }

                return
            }
        }

        var (_, name, clazz, clazzLevel) = tablistRegex.find(tabName.removeFormatting())?.destructured ?: return
        if (runPlayersNames.isEmpty()) name = mc.user.name
        val skin = if (runPlayersNames.isEmpty()) mc.player !!.skin.body.texturePath() else mc.connection?.getPlayerInfo(name)?.skin?.body?.texturePath() ?: DefaultPlayerSkin.getDefaultTexture()
        runPlayersNames[name] = skin
        if (clazz == "EMPTY") return

        dungeonTeammates.find { it.name == name }?.let { currentTeammate ->
            currentTeammate.clazz = if (clazz != "DEAD") Classes.getByName(clazz) else currentTeammate.clazz
            currentTeammate.clazzLvl = clazzLevel.romanToDecimal()
            currentTeammate.skin = skin
            currentTeammate.isDead = clazz == "DEAD"
        } ?: run {
            dungeonTeammates.add(
                DungeonPlayer(
                    name,
                    Classes.getByName(clazz),
                    clazzLevel.romanToDecimal(),
                    skin,
                    clazz == "DEAD",
                )
            )
        }

        thePlayer = dungeonTeammates.find { it.name == mc.user.name }
        dungeonTeammatesNoSelf = dungeonTeammates.filter { it != thePlayer }
        leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }

        dungeonTeammates.onEach { teammate ->
            teammate.entity = mc.level?.players()?.find { it.name.string == teammate.name } ?: teammate.entity
        }
    }

    private fun updatePuzzleCount(tabName: String) {
        if (maxPuzzleCount != 0) return
        if (tabName.contains("Puzzles: ")) {
            maxPuzzleCount = puzzleCountRegex.matchEntire(tabName)?.groups?.get(1)?.value?.toIntOrNull() ?: maxPuzzleCount
            repeat(maxPuzzleCount) { puzzles.add(Puzzle.UNKNOWN) }
        }
    }

    private fun updatePuzzles(tabName: String) {
        val line = tabName.removeFormatting()

        val name = puzzleRegex.find(line)?.destructured?.component1() ?: return
        val newState = when {
            "✦" in line && "???" in line -> RoomState.UNOPENED
            "✦" in line -> RoomState.DISCOVERED
            "✖" in line -> RoomState.FAILED
            "✔" in line -> RoomState.GREEN
            else -> return
        }

        val detected = Puzzle.fromName(name) ?: return

        val puzzle = puzzles.find { it == detected && it != Puzzle.UNKNOWN } ?: run {
            if (detected != Puzzle.UNKNOWN) puzzles.find { it == Puzzle.UNKNOWN }?.let {
                puzzles.remove(it)
                puzzles.add(detected)
                return@run detected
            }

            detected
        }

        if (puzzle != Puzzle.UNKNOWN && puzzle.state != newState) {
            puzzle.state = newState
        }
    }
}

