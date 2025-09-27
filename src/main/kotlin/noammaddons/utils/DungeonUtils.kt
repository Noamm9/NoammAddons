package noammaddons.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.block.BlockSkull
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.*
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mayorData
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.scope
import noammaddons.events.*
import noammaddons.events.EventDispatcher.postAndCatch
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.dungeons.ScoreCalculator
import noammaddons.features.impl.dungeons.dmap.core.ClearInfo
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapPlayer
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.hud.RunSplits.currentTime
import noammaddons.utils.ActionBarParser.maxSecrets
import noammaddons.utils.ActionBarParser.secrets
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object DungeonUtils {
    private val tablistRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$") // https://regex101.com/r/gv7bOe/1
    private val puzzleRegex = Regex(" (.+): \\[[✦✔✖].+")
    private val deathRegex = Regex("^ ☠ (?:You were|(?<username>\\w+)) (?<reason>.+?)(?: and became a ghost)?\\.\$") // https://regex101.com/r/Yc3HhV/4
    private val keyPickupRegex = Regex("§r§e§lRIGHT CLICK §r§7on §r§7.+?§r§7 to open it\\. This key can only be used to open §r§a(?<num>\\d+)§r§7 door!§r")
    private val witherDoorOpenedRegex = Regex("^(?:\\[.+?] )?(?<name>\\w+) opened a WITHER door!$")
    private const val bloodOpenedString = "§r§cThe §r§c§lBLOOD DOOR§r§c has been opened!§r"
    private val watcherMessageRegex = Regex("^\\[BOSS\\] The Watcher: .+$")
    private val runEndRegex = Regex("^\\s*(Master Mode)? ?(?:The)? Catacombs - (Floor (.{1,3})|Entrance)\$") // https://regex101.com/r/W4UjWQ/3

    const val WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23"
    const val REDSTONE_KEY_ID = "fed95410-aba1-39df-9b95-1d4f361eb66e"

    val runPlayersNames = mutableMapOf<String, ResourceLocation>()
    var dungeonTeammates = mutableListOf<DungeonPlayer>()
    var dungeonTeammatesNoSelf = listOf<DungeonPlayer>()
    var leapTeammates = listOf<DungeonPlayer>()
    var thePlayer: DungeonPlayer? = null

    data class Puzzle(val name: String, var state: RoomState)

    var puzzles = mutableListOf<Puzzle>()

    val dungeonStarted get() = dungeonTeammates.isNotEmpty()
    var dungeonStartTime: Long? = null
    var dungeonEnded = false

    var bloodOpenTime: Long? = null
    var watcherClearTime: Long? = null
    var watcherSpawnTime: Long? = null
    var bossEntryTime: Long? = null
    var dungeonEndTime: Long? = null

    var lastDoorOpenner: DungeonPlayer? = null

    val dungeonItemDrops = listOf(
        "Health Potion VIII Splash Potion", "Healing Potion 8 Splash Potion",
        "Healing Potion VIII Splash Potion", "Healing VIII Splash Potion",
        "Healing 8 Splash Potion", "Decoy", "Inflatable Jerry", "Spirit Leap",
        "Trap", "Training Weights", "Defuse Kit", "Dungeon Chest Key",
        "Treasure Talisman", "Revive Stone", "Architect's First Draft",
        "Secret Dye", "Candycomb"
    )

    enum class Classes(val color: Color) {
        Empty(Color(0, 0, 0)),
        Archer(Color(125, 0, 0)),
        Berserk(Color(205, 106, 0)),
        Healer(Color(123, 0, 123)),
        Mage(Color(0, 185, 185)),
        Tank(Color(0, 125, 0));

        companion object {
            fun getByName(name: String) = entries.first { it.name.lowercase() == name.lowercase() }
            fun getColorCode(clazz: Classes): String {
                return when (clazz) {
                    Archer -> "§4"
                    Berserk -> "§6"
                    Healer -> "§5"
                    Mage -> "§3"
                    Tank -> "§2"
                    else -> "§7"
                }
            }
        }
    }

    data class DungeonPlayer(
        var name: String,
        var clazz: Classes,
        var clazzLvl: Int,
        var skin: ResourceLocation = ResourceLocation("textures/entity/steve.png"),
        var isDead: Boolean = false,
    ) {
        val entity: EntityPlayer? get() = mc.theWorld.getPlayerEntityByName(name)
        val mapIcon = DungeonMapPlayer(this, skin)
        val clearInfo = ClearInfo()
    }

    enum class Blessing(
        var regex: Regex,
        val displayString: String,
        var current: Int = 0
    ) {
        POWER(Regex("Blessing of Power (X{0,3}(IX|IV|V?I{0,3}))"), "Power"),
        LIFE(Regex("Blessing of Life (X{0,3}(IX|IV|V?I{0,3}))"), "Life"),
        WISDOM(Regex("Blessing of Wisdom (X{0,3}(IX|IV|V?I{0,3}))"), "Wisdom"),
        STONE(Regex("Blessing of Stone (X{0,3}(IX|IV|V?I{0,3}))"), "Stone"),
        TIME(Regex("Blessing of Time (V)"), "Time");

        companion object {
            fun reset() = entries.forEach { it.current = 0 }
        }
    }

    @JvmStatic
    fun isSecret(pos: BlockPos): Boolean {
        val block = getBlockAt(pos)

        return when {
            block is BlockSkull -> (mc.theWorld?.getTileEntity(pos) as? TileEntitySkull)?.playerProfile?.id.toString().equalsOneOf(WITHER_ESSENCE_ID, REDSTONE_KEY_ID)
            block.equalsOneOf(Blocks.chest, Blocks.trapped_chest, Blocks.lever) -> true
            else -> false
        }
    }

    fun isPaul(): Boolean {
        if (ScoreCalculator.forcePaul.value) return true
        val mayorPerks = mutableListOf<DataClasses.ApiMayor.Perk>()
        mayorData?.mayor?.perks?.let { mayorPerks.addAll(it) }
        mayorData?.mayor?.minister?.perks?.let { mayorPerks.addAll(it) }
        return mayorPerks.any { it.name.contains("EZPZ") }
    }

    private fun updateDungeonTeammates(tabListEntries: List<String>) {
        if (DevOptions.devMode) {
            listOf(
                DungeonPlayer("Noamm", Classes.Mage, 50, isDead = false),
                DungeonPlayer("Noamm9", Classes.Archer, 50, isDead = false),
                DungeonPlayer("NoammALT", Classes.Healer, 50, isDead = true),
                DungeonPlayer("NoamIsSad", Classes.Tank, 50, isDead = false),
            ).let { list ->
                dungeonTeammates.clear()
                dungeonTeammates.addAll(list)

                thePlayer = dungeonTeammates.find { it.entity == mc.thePlayer }
                dungeonTeammatesNoSelf = dungeonTeammates.filterNot { it == thePlayer }
                leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }


                return
            }
        }

        for (line in tabListEntries) {
            var (sbLvl, name, clazz, clazzLevel) = tablistRegex.find(line.removeFormatting())?.destructured ?: continue
            if (runPlayersNames.isEmpty()) name = mc.session.username
            val skin = if (runPlayersNames.isEmpty()) mc.thePlayer.locationSkin else mc.netHandler?.getPlayerInfo(name)?.locationSkin ?: ResourceLocation("textures/entity/steve.png")
            runPlayersNames[name] = skin
            if (clazz == "EMPTY") continue

            dungeonTeammates.find { it.name == name }?.let { currentTeammate ->
                currentTeammate.clazz = if (clazz != "DEAD") Classes.getByName(clazz) else currentTeammate.clazz
                currentTeammate.clazzLvl = clazzLevel.romanToDecimal()
                currentTeammate.skin = skin
                if (currentTeammate != thePlayer) currentTeammate.isDead = clazz == "DEAD"
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
        }

        thePlayer = dungeonTeammates.find { it.name == mc.session.username }
        dungeonTeammatesNoSelf = dungeonTeammates.filter { it != thePlayer }
        leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }

        dungeonTeammatesNoSelf.filterNot { it.isDead }.forEachIndexed { i, teammate ->
            teammate.mapIcon.icon = "icon-$i"
        }

        thePlayer?.takeIf { ! it.isDead }?.mapIcon?.apply {
            val last = dungeonTeammates.filterNot { it.isDead }.lastIndex
            icon = "icon-$last"
        }
    }

    private fun updatePuzzles(tabListEntries: List<String>) {
        tabListEntries.forEach { line ->
            val name = puzzleRegex.find(line)?.destructured?.component1()?.takeUnless { "?" in it } ?: return@forEach
            val state = when {
                "✔" in line -> RoomState.GREEN
                "✖" in line -> RoomState.FAILED
                "✦" in line -> RoomState.DISCOVERED
                else -> RoomState.UNOPENED
            }
            val puzzle = puzzles.find { it.name == name } ?: run {
                val newPuzzle = Puzzle(name, state)
                puzzles.add(newPuzzle)
                postAndCatch(DungeonEvent.PuzzleEvent.Discovered(name))
                return@forEach
            }

            postAndCatch(
                when {
                    puzzle.state == RoomState.DISCOVERED && state == RoomState.GREEN -> DungeonEvent.PuzzleEvent.Completed(name)
                    puzzle.state != RoomState.DISCOVERED && state == RoomState.DISCOVERED -> DungeonEvent.PuzzleEvent.Reset(name)
                    puzzle.state == RoomState.DISCOVERED && state == RoomState.FAILED -> DungeonEvent.PuzzleEvent.Failed(name)
                    else -> return@forEach
                }
            )

            puzzle.state = state
        }
    }

    @SubscribeEvent
    fun onPacket(event: PostPacketEvent.Received) {
        if (! inDungeon) return

        when (val packet = event.packet) {
            is S38PacketPlayerListItem -> {
                if (! packet.action.equalsOneOf(S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, S38PacketPlayerListItem.Action.ADD_PLAYER)) return
                val tabListEntries = packet.entries?.mapNotNull { it.displayName?.noFormatText } ?: return
                updateDungeonTeammates(tabListEntries)
                updatePuzzles(tabListEntries)
            }

            is S47PacketPlayerListHeaderFooter -> Blessing.entries.forEach { blessing ->
                blessing.regex.find(packet.footer?.unformattedText?.removeFormatting() ?: return@forEach)?.let {
                    blessing.current = it.groupValues[1].romanToDecimal()
                }
            }

            is S2FPacketSetSlot -> {
                if (packet.func_149175_c() != 0 || packet.func_149173_d() != 36) return
                val wasDead = mc.thePlayer.inventory.getStackInSlot(0)?.skyblockID == "HAUNT_ABILITY"
                val isNowDead = packet.func_149174_e()?.skyblockID == "HAUNT_ABILITY"
                if (wasDead != isNowDead) thePlayer?.isDead = isNowDead
            }
        }
    }

    @SubscribeEvent
    fun onRoomStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (lastDoorOpenner == null) return
        if (event.room.data.type != RoomType.BLOOD) return
        if (! event.newState.equalsOneOf(RoomState.DISCOVERED, RoomState.CLEARED, RoomState.GREEN)) return
        lastDoorOpenner = null
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        val text = event.component.formattedText
        val unformatted = text.removeFormatting()

        when {
            unformatted.matches(runEndRegex) -> {
                dungeonEnded = true
                dungeonEndTime = currentTime
                scope.launch { postAndCatch(DungeonEvent.RunEndedEvent()) }
            }

            text == bloodOpenedString -> DungeonInfo.keys --

            "§r§c ☠" in text && "reconnected" !in unformatted -> {
                val match = deathRegex.find(unformatted) ?: return
                val username = match.groups["username"]?.value?.takeUnless { it == "You" } ?: mc.session.username
                val reason = match.groups["reason"]?.value ?: ""
                scope.launch {
                    while (thePlayer == null) delay(1)
                    if (username == mc.session.username) thePlayer?.isDead = true
                    postAndCatch(DungeonEvent.PlayerDeathEvent(username, reason))
                }
            }

            unformatted == "[BOSS] The Watcher: You have proven yourself. You may pass." -> {
                DungeonInfo.uniqueRooms.find { it.mainRoom.data.type == RoomType.BLOOD }?.mainRoom?.state = RoomState.GREEN
                watcherClearTime = currentTime
            }

            unformatted == "[BOSS] The Watcher: That will be enough for now." -> {
                DungeonInfo.uniqueRooms.find { it.mainRoom.data.type == RoomType.BLOOD }?.mainRoom?.state = RoomState.CLEARED
                watcherSpawnTime = currentTime
            }

            watcherMessageRegex.matches(unformatted) && bloodOpenTime == null -> {
                bloodOpenTime = currentTime
            }

            unformatted == "[NPC] Mort: Here, I found this map when I first entered the dungeon." -> scope.launch {
                dungeonStartTime = currentTime
                while (thePlayer == null) delay(1)
                postAndCatch(DungeonEvent.RunStatedEvent())
            }

            else -> {
                witherDoorOpenedRegex.find(unformatted)?.destructured?.let { (name) ->
                    lastDoorOpenner = dungeonTeammates.find { it.name == name }
                    DungeonInfo.keys --
                    return
                }

                keyPickupRegex.find(text)?.destructured?.let { (num) ->
                    DungeonInfo.keys += num.toInt()
                    return
                }
            }
        }
    }


    @SubscribeEvent
    fun reset(event: WorldUnloadEvent) {
        dungeonTeammates.clear()
        dungeonTeammatesNoSelf = emptyList()
        leapTeammates = emptyList()
        thePlayer = null
        maxSecrets = null
        secrets = null
        puzzles.clear()
        dungeonEnded = false
        runPlayersNames.clear()
        dungeonStartTime = null
        bloodOpenTime = null
        watcherClearTime = null
        watcherSpawnTime = null
        bossEntryTime = null
        dungeonEndTime = null
        Blessing.reset()
    }
}
