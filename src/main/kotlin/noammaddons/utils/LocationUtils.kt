package noammaddons.utils

import gg.essential.api.EssentialAPI
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.*
import noammaddons.events.Tick
import noammaddons.events.WorldUnloadEvent
import noammaddons.noammaddons.Companion.config
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
    fun check(event: Tick) {
        TickTimer ++
        if (TickTimer != 20) return

        if (config.DevMode) setDevModeValues()
        else updateSkyblockAndDungeonStatus()

        TickTimer = 0
    }

    @SubscribeEvent
    fun onEvent(event: Event) {
        if (event is WorldUnloadEvent) reset()
        if (event is ClientDisconnectionFromServerEvent) reset()
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

    private fun updateSkyblockAndDungeonStatus() {
        inSkyblock = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)?.name == "SBScoreboard"

        if (inSkyblock) {
            inBoss = isInBossRoom()
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


    private fun getPhase(): Int? {
        if (dungeonFloorNumber != 7 && ! inBoss) return null

        val playerPosition = mc.thePlayer?.positionVector ?: return null
        val corner1 = Vec3(- 8.0, 254.0, 147.0)
        val corner2 = Vec3(134.0, 0.0, - 8.0)

        if (isCoordinateInsideBox(playerPosition, corner1, corner2)) {
            return when {
                playerPosition.yCoord > 210 -> 1
                playerPosition.yCoord > 155 -> 2
                playerPosition.yCoord > 100 -> 3
                playerPosition.yCoord > 45 -> 4
                else -> 5
            }
        }

        return null
    }

    private val P3Sections = listOf(
        Pair(Vec3(90.0, 158.0, 123.0), Vec3(111.0, 105.0, 32.0)),  //  1
        Pair(Vec3(16.0, 158.0, 122.0), Vec3(111.0, 105.0, 143.0)), //  2
        Pair(Vec3(19.0, 158.0, 48.0), Vec3(- 3.0, 106.0, 142.0)),  //  3
        Pair(Vec3(91.0, 158.0, 50.0), Vec3(- 3.0, 106.0, 30.0))    //  4
    )

    private fun getP3Section_(): Int? {
        if (F7Phase != 3) return null
        val pos = mc.thePlayer?.positionVector ?: return null

        P3Sections.forEachIndexed { i, (one, two) ->
            if (isCoordinateInsideBox(pos, one, two)) {
                return i + 1
            }
        }

        return null
    }

    private val bossRoomCorners = mapOf(
        7 to Pair(Vec3(- 8.0, 0.0, - 8.0), Vec3(134.0, 254.0, 147.0)),
        6 to Pair(Vec3(- 40.0, 51.0, - 8.0), Vec3(22.0, 110.0, 134.0)),
        5 to Pair(Vec3(- 40.0, 53.0, - 8.0), Vec3(50.0, 112.0, 118.0)),
        4 to Pair(Vec3(- 40.0, 53.0, - 40.0), Vec3(134.0, 254.0, 147.0)),
        3 to Pair(Vec3(- 40.0, 0.0, - 40.0), Vec3(42.0, 118.0, 73.0)),
        2 to Pair(Vec3(- 40.0, 99.0, - 40.0), Vec3(42.0, 118.0, 73.0)),
        1 to Pair(Vec3(- 14.0, 146.0, 49.0), Vec3(24.0, 54.0, 54.0))
    )

    private fun isInBossRoom(): Boolean {
        val playerCoords = mc.thePlayer?.positionVector ?: return false
        val corners = bossRoomCorners[dungeonFloorNumber] ?: return false
        return isCoordinateInsideBox(playerCoords, corners.first, corners.second)
    }
}
