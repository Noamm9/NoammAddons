package noammaddons.utils

import gg.essential.api.EssentialAPI
import net.minecraft.util.BlockPos
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.*
import noammaddons.events.Tick
import noammaddons.events.WorldUnloadEvent
import noammaddons.features.impl.DevOptions
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.isCoordinateInsideBox
import noammaddons.utils.ScoreboardUtils.cleanSB
import noammaddons.utils.ScoreboardUtils.sidebarLines
import noammaddons.utils.TablistUtils.getTabList


object LocationUtils {
    private val WorldNameRegex = Regex("(Area|Dungeon): ([\\w ]+)")
    private var TickTimer = 0

    @JvmStatic
    val onHypixel get() = EssentialAPI.getMinecraftUtil().isHypixel()

    @JvmField
    var inSkyblock = false

    @JvmField
    var inDungeon = false

    @JvmField
    var dungeonFloor: String? = null

    @JvmField
    var dungeonFloorNumber: Int? = null

    @JvmStatic
    val isMasterMode get() = dungeonFloor?.startsWith("M") == true

    @JvmField
    var inBoss = false

    @JvmField
    var P3Section: Int? = null

    @JvmField
    var F7Phase: Int? = null

    @JvmField
    var world: WorldType? = null

    enum class WorldType(val string: String) {
        DungeonHub("Dungeon Hub"),
        Catacombs("Catacombs"),
        Home("Private Island"),
        Hub("Hub"),
        Park("The Park"),
        SpiderDen("Spider"),
        End("The End"),
        CrimonIsle("Crimson Isle"),
        GoldMine("Gold Mine"),
        DeepCaverns("Deep Caverns"),
        DwarvenMines("Dwarven Mines"),
        CrystalHollows("Crystal Hollows"),
        TheBarn("The Farming Islands"),
        BackwaterBayou("Backwater Bayou"),
        Garden("Garden");

        companion object {
            fun get(string: String?) = entries.find { it.string == string }
        }
    }

    @SubscribeEvent
    fun onEvent(event: Event) {
        when (event) {
            is Tick -> {
                TickTimer ++
                if (TickTimer == 20) return
                if (DevOptions.devMode) setDevModeValues()
                else updateLocation()
                TickTimer = 0
            }

            is WorldUnloadEvent -> reset()
            is WorldEvent.Load -> reset()
            is ClientDisconnectionFromServerEvent -> reset()
            else -> return
        }
    }


    private fun reset() {
        inSkyblock = false
        inDungeon = false
        dungeonFloor = null
        dungeonFloorNumber = null
        inBoss = false
        P3Section = null
        F7Phase = null
        world = null
    }

    private fun setDevModeValues() {
        inSkyblock = true
        inDungeon = true
        dungeonFloor = "F7"
        dungeonFloorNumber = 7
        F7Phase = getPhase()
        P3Section = getP3Section_()
        inBoss = isInBossRoom()
    }

    private fun updateLocation() {
        inSkyblock = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)?.name == "SBScoreboard"

        if (inSkyblock) {
            inBoss = isInBossRoom()
            if (inBoss && DungeonUtils.bossEntryTime == null) {
                DungeonUtils.bossEntryTime = System.currentTimeMillis()
            }
            F7Phase = getPhase()
            P3Section = getP3Section_()
            world = updateWorldName()
        }

        if (! inDungeon) updateDungeonStatus()
    }

    private fun updateWorldName() = WorldType.get(
        WorldNameRegex.find(
            getTabList.joinToString { it.second.removeFormatting() }
        )?.destructured?.component2()
    )

    private fun updateDungeonStatus() = sidebarLines.find {
        cleanSB(it).run {
            contains("The Catacombs (") && ! contains("Queue")
        }
    }?.run {
        inDungeon = true
        dungeonFloor = cleanSB(this).substringAfter("(").substringBefore(")")
        dungeonFloorNumber = dungeonFloor?.lastOrNull()?.digitToIntOrNull() ?: 0
    }

    fun isInHubCarnival(): Boolean {
        return isCoordinateInsideBox(
            ServerPlayer.player.takeIf { it.initialized }?.getPos() ?: return false,
            BlockPos(- 123, 100, 36), BlockPos(- 64, 70, - 31)
        )
    }

    private fun getPhase(): Int? {
        if (dungeonFloorNumber != 7 || ! inBoss) return null
        val playerPosition = ServerPlayer.player.takeIf { it.initialized }?.getPos() ?: return null

        return when {
            playerPosition.y > 210 -> 1
            playerPosition.y > 155 -> 2
            playerPosition.y > 100 -> 3
            playerPosition.y > 45 -> 4
            else -> 5
        }
    }

    private val P3Sections = listOf(
        Pair(BlockPos(90, 158, 123), BlockPos(111, 105, 32)),  //  1
        Pair(BlockPos(16, 158, 122), BlockPos(111, 105, 143)), //  2
        Pair(BlockPos(19, 158, 48), BlockPos(- 3, 106, 142)),  //  3
        Pair(BlockPos(91, 158, 50), BlockPos(- 3, 106, 30))    //  4
    )

    private fun getP3Section_(): Int? {
        if (F7Phase != 3) return null
        val player = ServerPlayer.player.takeIf { it.initialized } ?: return null

        P3Sections.forEachIndexed { i, (one, two) ->
            if (isCoordinateInsideBox(player.getPos(), one, two)) {
                return i + 1
            }
        }

        return null
    }

    private val bossRoomCorners = mapOf(
        7 to Pair(BlockPos(- 8, 0, - 8), BlockPos(134, 254, 147)),
        6 to Pair(BlockPos(- 40, 51, - 8), BlockPos(22, 110, 134)),
        5 to Pair(BlockPos(- 40, 112, - 8), BlockPos(50, 53, 118)),
        4 to Pair(BlockPos(- 40, 112, - 40), BlockPos(50, 53, 47)),
        3 to Pair(BlockPos(- 40, 118, - 40), BlockPos(42, 64, 31)),
        2 to Pair(BlockPos(- 40, 99, - 40), BlockPos(24, 54, 59)),
        1 to Pair(BlockPos(- 14, 55, 49), BlockPos(- 72, 146, - 40))
    )

    private fun isInBossRoom(): Boolean {
        val player = ServerPlayer.player.takeIf { it.initialized } ?: return false
        val floor = dungeonFloorNumber ?: return false
        val corners = bossRoomCorners[floor] ?: return false
        return isCoordinateInsideBox(player.getPos(), corners.first, corners.second)
    }
}
