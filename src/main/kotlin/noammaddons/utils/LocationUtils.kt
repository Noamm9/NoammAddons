package noammaddons.utils

import gg.essential.api.EssentialAPI
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.events.Tick
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.isCoordinateInsideBox
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ScoreboardUtils.sidebarLines
import noammaddons.utils.TablistUtils.getTabList


object LocationUtils {
    private val WorldNameRegex = Regex("(Area|Dungeon): ([\\w ]+)")
    private var TickTimer = 0

    val onHypixel get() = EssentialAPI.getMinecraftUtil().isHypixel()
    var inSkyblock = false
    var inDungeons = false
    var dungeonFloor: Int? = null
    var inBoss = false
    var P3Section: Int? = null
    var F7Phase: Int? = null
    var WorldName: WorldType? = null

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
        Garden("Garden");

        companion object {
            fun get(string: String?) = entries.find { it.string == string }
        }
    }

    @SubscribeEvent
    fun get(event: Tick) {
        TickTimer ++
        if (TickTimer != 20) return


        if (false/*config.DevMode*/) {
            inSkyblock = true
            inDungeons = true
            dungeonFloor = 7
            F7Phase = 5
            P3Section = 4
            inBoss = true
        }
        else {
            inSkyblock = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)?.name == "SBScoreboard"

            if (inSkyblock) {
                inBoss = isInBossRoom()
                F7Phase = getPhase()
                P3Section = getP3Section_()
                WorldName = WorldType.get(WorldNameRegex.find(
                    getTabList.joinToString { it.second.removeFormatting() }
                )?.destructured?.component2())
            }

            if (! inDungeons) {
                sidebarLines.find {
                    ScoreboardUtils.cleanSB(it).run {
                        contains("The Catacombs (") && ! contains("Queue")
                    }
                }?.let {
                    inDungeons = true
                    dungeonFloor = it.substringBefore(")").lastOrNull()?.digitToIntOrNull() ?: 0
                }
            }
        }
        TickTimer = 0
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        inSkyblock = false
        inDungeons = false
        dungeonFloor = null
        inBoss = false
        P3Section = null
        F7Phase = null
        WorldName = null
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        inSkyblock = false
        inDungeons = false
        dungeonFloor = null
        inBoss = false
        P3Section = null
        F7Phase = null
        WorldName = null
    }


    private fun getPhase(): Int? {
        if (dungeonFloor != 7 && ! inBoss) return null

        val playerPosition = Player?.positionVector ?: return null
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

        Player?.positionVector?.run {
            P3Sections.forEachIndexed { index, section ->
                return if (isCoordinateInsideBox(this, section.first, section.second)) {
                    index + 1
                }
                else null
            }
        }

        return null
    }

    // todo: add floor 1 & 2
    private val bossRoomCorners = mapOf(
        7 to Pair(Vec3(- 8.0, 0.0, - 8.0), Vec3(134.0, 254.0, 147.0)),
        6 to Pair(Vec3(- 40.0, 51.0, - 8.0), Vec3(22.0, 110.0, 134.0)),
        5 to Pair(Vec3(- 40.0, 53.0, - 8.0), Vec3(50.0, 112.0, 118.0)),
        4 to Pair(Vec3(- 40.0, 53.0, - 40.0), Vec3(134.0, 254.0, 147.0)),
        3 to Pair(Vec3(- 40.0, 0.0, - 40.0), Vec3(42.0, 118.0, 73.0))
    )

    private fun isInBossRoom(): Boolean {
        val playerCoords = Player?.positionVector ?: return false
        val corners = bossRoomCorners[dungeonFloor] ?: return false

        return isCoordinateInsideBox(playerCoords, corners.first, corners.second)
    }

    fun isCoordinateInsideBoss(vec: Vec3): Boolean {
        val corners = bossRoomCorners[dungeonFloor] ?: return false

        return isCoordinateInsideBox(vec, corners.first, corners.second)
    }
}
