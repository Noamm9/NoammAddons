package noammaddons.utils

import gg.essential.elementa.state.BasicState
import net.minecraft.block.BlockSkull
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.events.RegisterEvents.postAndCatch
import noammaddons.features.dungeons.dmap.core.DungeonMapPlayer
import noammaddons.features.dungeons.dmap.core.map.*
import noammaddons.features.dungeons.dmap.handlers.DungeonInfo
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ActionBarParser.maxSecrets
import noammaddons.utils.ActionBarParser.secrets
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.TablistUtils.getTabList
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object DungeonUtils {
    // https://regex101.com/r/gv7bOe/1
    private val tablistRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$")
    val partyCountPattern = Regex("§r {9}§r§b§lParty §r§f\\((?<count>[1-5])\\)§r")
    private val missingPuzzlePattern = Regex("§r (?<puzzle>.+): §r§7\\[§r§6§l✦§r§7] ?§r")
    private val deathRegex = Regex("§r§c ☠ §r§7(?:You were |(?:§.)+(?<username>\\w+)§r)(?<reason>.*) and became a ghost§r§7\\.§r")
    private val keyPickupRegex = Regex("§r§e§lRIGHT CLICK §r§7on §r§7.+?§r§7 to open it\\. This key can only be used to open §r§a(?<num>\\d+)§r§7 door!§r")
    private val witherDoorOpenedRegex = Regex("^(?:\\[.+?] )?(?<name>\\w+) opened a WITHER door!$")
    private const val bloodOpenedString = "§r§cThe §r§c§lBLOOD DOOR§r§c has been opened!§r"

    const val WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23"
    const val REDSTONE_KEY_ID = "fed95410-aba1-39df-9b95-1d4f361eb66e"

    val runPlayersNames = mutableMapOf<String, ResourceLocation>()
    var dungeonTeammates = mutableListOf<DungeonPlayer>()
    var dungeonTeammatesNoSelf = listOf<DungeonPlayer>()
    var leapTeammates = listOf<DungeonPlayer>()
    var thePlayer: DungeonPlayer? = null

    val missingPuzzles = hashSetOf<String>()
    val completedPuzzles = hashSetOf<String>()

    val dungeonStarted get() = dungeonTeammates.isNotEmpty()
    var dungeonEnded = BasicState(false)

    var lastDoorOpenner: DungeonPlayer? = null

    val dungeonItemDrops = listOf(
        "Health Potion VIII Splash Potion", "Healing Potion 8 Splash Potion",
        "Healing Potion VIII Splash Potion", "Healing VIII Splash Potion",
        "Healing 8 Splash Potion", "Decoy", "Inflatable Jerry", "Spirit Leap",
        "Trap", "Training Weights", "Defuse Kit", "Dungeon Chest Key",
        "Treasure Talisman", "Revive Stone", "Architect's First Draft"
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
        }
    }

    data class DungeonPlayer(
        var name: String,
        var clazz: Classes,
        var clazzLvl: Int,
        var locationSkin: ResourceLocation = ResourceLocation("textures/entity/steve.png"),
        var entity: EntityPlayer? = null,
        var isDead: Boolean = false,
        var mapIcon: DungeonMapPlayer? = null
    )

    @JvmStatic
    fun isSecret(pos: BlockPos): Boolean {
        val block = getBlockAt(pos)

        return when {
            block is BlockSkull -> (mc.theWorld?.getTileEntity(pos) as? TileEntitySkull)?.playerProfile?.id.toString().equalsOneOf(WITHER_ESSENCE_ID, REDSTONE_KEY_ID)
            block.equalsOneOf(Blocks.chest, Blocks.trapped_chest, Blocks.lever) -> true
            else -> false
        }
    }

    private fun updateDungeonTeammates() {
        if (config.DevMode) {
            listOf(
                DungeonPlayer("Noamm", Classes.Mage, 50, isDead = false),
                DungeonPlayer("Noamm9", Classes.Archer, 50, isDead = false),
                DungeonPlayer("NoammALT", Classes.Healer, 50, isDead = true, entity = mc.theWorld.getPlayerEntityByName("NoammALT")),
                DungeonPlayer("NoamIsSad", Classes.Tank, 50, isDead = false),
            ).let { list ->
                dungeonTeammates.clear()
                dungeonTeammates.addAll(list)

                thePlayer = dungeonTeammates.find { it.entity == mc.thePlayer }
                thePlayer?.isDead = mc.thePlayer.inventory.getStackInSlot(0)?.SkyblockID == "HAUNT_ABILITY"
                dungeonTeammatesNoSelf = dungeonTeammates.filterNot { it == thePlayer }.toMutableList()
                leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }.toMutableList()
            }
            return
        }

        val tabList = getTabList.takeIf { it.size >= 18 || it[0].second.contains("§r§b§lParty §r§f(") } ?: return
        for ((networkPlayerInfo, line) in tabList) {
            val (all, sbLvl, name, clazz, clazzLevel) = tablistRegex.find(line.removeFormatting())?.groupValues ?: continue
            runPlayersNames[name] = networkPlayerInfo.locationSkin
            if (clazz == "EMPTY") continue

            val currentTeammate = dungeonTeammates.find { it.name == name }

            if (currentTeammate != null) {
                currentTeammate.clazz = if (clazz != "DEAD") Classes.getByName(clazz) else currentTeammate.clazz
                currentTeammate.clazzLvl = clazzLevel.romanToDecimal()
                currentTeammate.locationSkin = networkPlayerInfo.locationSkin
                currentTeammate.entity = mc.theWorld.getPlayerEntityByName(name)
                currentTeammate.isDead = clazz == "DEAD"
            }
            else dungeonTeammates.add(
                DungeonPlayer(
                    name,
                    Classes.getByName(clazz),
                    clazzLevel.romanToDecimal(),
                    networkPlayerInfo.locationSkin,
                    mc.theWorld.getPlayerEntityByName(name),
                    clazz == "DEAD"
                )
            )
        }

        thePlayer = dungeonTeammates.find { it.entity == mc.thePlayer }
        dungeonTeammatesNoSelf = dungeonTeammates.filter { it != thePlayer }.toMutableList()
        leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }.toMutableList()
        if (runPlayersNames.size != dungeonTeammates.size) {
            runPlayersNames.clear()
            dungeonTeammates.clear()
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        val text = event.component.formattedText
        val unformatted = text.removeFormatting()

        when {
            unformatted == "                             > EXTRA STATS <" -> dungeonEnded.set(true)
            text == bloodOpenedString -> DungeonInfo.keys --
            text.endsWith(" and became a ghost§r§7.§r") -> {
                val match = deathRegex.find(text) ?: return
                val username = match.groups["username"]?.value ?: mc.thePlayer.name
                val reason = match.groups["reason"]?.value
                postAndCatch(DungeonEvent.PlayerDeathEvent(username, reason))
            }

            unformatted == "[BOSS] The Watcher: You have proven yourself. You may pass." -> DungeonInfo.dungeonList.find { it is Room && it.data.type == RoomType.BLOOD }?.state = RoomState.GREEN
            unformatted == "[BOSS] The Watcher: That will be enough for now." -> DungeonInfo.dungeonList.find { it is Room && it.data.type == RoomType.BLOOD }?.state = RoomState.CLEARED

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
        missingPuzzles.clear()
        completedPuzzles.clear()
        dungeonEnded.set(false)
        runPlayersNames.clear()
    }


    init {
        loop(250) {
            if (! inDungeon) return@loop
            if (mc.theWorld == null) return@loop
            if (mc.thePlayer == null) return@loop

            updateDungeonTeammates()

            if (lastDoorOpenner == null) return@loop
            val bloodRoom = DungeonInfo.dungeonList.find { it is Room && it.data.type == RoomType.BLOOD }
            if (bloodRoom?.state.equalsOneOf(RoomState.DISCOVERED, RoomState.CLEARED, RoomState.GREEN)) {
                lastDoorOpenner = null
            }
        }

        loop(250) {
            if (! inDungeon) return@loop
            val localMissingPuzzles = hashSetOf<String>()

            for ((_, str) in getTabList) {
                if (str.contains("✦")) {
                    val matcher = missingPuzzlePattern.find(str)
                    if (matcher != null) {
                        val puzzleName = matcher.groups["puzzle"] !!.value
                        if (puzzleName != "???") {
                            localMissingPuzzles.add(puzzleName)
                        }
                    }
                }
            }

            if (missingPuzzles.size != localMissingPuzzles.size || ! missingPuzzles.containsAll(localMissingPuzzles)) {
                val newPuzzles = localMissingPuzzles.filter { it !in missingPuzzles }
                val localCompletedPuzzles = missingPuzzles.filter { it !in localMissingPuzzles }
                val resetPuzzles = localMissingPuzzles.filter { it in completedPuzzles }

                resetPuzzles.forEach { postAndCatch(DungeonEvent.PuzzleEvent.Reset(it)) }
                newPuzzles.forEach { postAndCatch(DungeonEvent.PuzzleEvent.Discovered(it)) }
                localCompletedPuzzles.forEach { postAndCatch(DungeonEvent.PuzzleEvent.Completed(it)) }
                missingPuzzles.clear()
                missingPuzzles.addAll(localMissingPuzzles)
                completedPuzzles.clear()
                completedPuzzles.addAll(localCompletedPuzzles)
            }
        }
    }
}
