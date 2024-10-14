package noammaddons.utils

import gg.essential.api.EssentialAPI
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.RenderOverlay
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.isCoordinateInsideBox
import noammaddons.utils.ScoreboardUtils.sidebarLines
import noammaddons.utils.TablistUtils.getTabList
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.events.Tick
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.Utils.isNull


object LocationUtils {
	private val WorldNameRegex = Regex("(Area|Dungeon): ([\\w ]+)")
    private var tickCount = 0
	
	val onHypixel get() = EssentialAPI.getMinecraftUtil().isHypixel()
    var inSkyblock = false
    var inDungeons = false
    var dungeonFloor: Int? = null
    var inBoss = false
    var P3Section: Int? = null
    var F7Phase: Int? = null
    var WorldName: String? = null


    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onTick(event: Tick) {
        if (mc.theWorld.isNull()) return
        tickCount++
        if (tickCount % 20 != 0) return

        if (config.DevMode) {
            inSkyblock = true
            inDungeons = true
            dungeonFloor = 7
            F7Phase = 5
            P3Section = 1
            inBoss = true
        }
        
        else {
            inSkyblock = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)?.name == "SBScoreboard"

            if (!inDungeons) {
                sidebarLines.find {
                    ScoreboardUtils.cleanSB(it).run {
                        contains("The Catacombs (") && !contains("Queue")
                    }
                }?.let {
                    inDungeons = true
                    dungeonFloor = it.substringBefore(")").lastOrNull()?.digitToIntOrNull() ?: 0
                }
            }
	        
	        if (inSkyblock) {
		        inBoss = isInBossRoom()
		        F7Phase = getPhase()
		        P3Section = getP3Sectionn()
		        WorldName = WorldNameRegex.find(
			        getTabList.joinToString { it.second.removeFormatting() }
				)?.destructured?.component2()
	        }
        }
        tickCount = 0
    }
	
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onWorldUnload(event: WorldEvent.Unload) {
        inDungeons = false
        dungeonFloor = -1
        inBoss = false
        P3Section = null
        F7Phase = null
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        inSkyblock = false
        inDungeons = false
        dungeonFloor = -1
        inBoss = false
        P3Section = null
        F7Phase = null
        WorldName = null
    }

	
    private fun getPhase(): Int? {
        if (dungeonFloor != 7 && !inBoss) return null

        val playerPosition = Player?.positionVector ?: return null
        val corner1 = Vec3(-8.0, 254.0, 147.0)
        val corner2 = Vec3(134.0, 0.0, -8.0)
        var inPhase: Int? = null


        if (isCoordinateInsideBox(playerPosition, corner1, corner2)) {
            inPhase = when {
                playerPosition.yCoord > 210 -> 1
                playerPosition.yCoord > 155 -> 2
                playerPosition.yCoord > 100 -> 3
                playerPosition.yCoord > 45  -> 4
                else -> 5
            }
        }

        return inPhase
    }

    private val P3Sections = listOf(
        Pair(Vec3(90.0, 158.0, 123.0), Vec3(111.0, 105.0, 32.0)), // 1
        Pair(Vec3(16.0, 158.0, 122.0), Vec3(111.0, 105.0, 143.0)), // 2
        Pair(Vec3(19.0, 158.0, 48.0), Vec3(-3.0, 106.0, 142.0)), // 3
        Pair(Vec3(91.0, 158.0, 50.0), Vec3(-3.0, 106.0, 30.0))  // 4
    )

    private fun getP3Sectionn(): Int? {
        if (F7Phase != 3) return null

        val playerCoords = Player?.positionVector ?: return 1

        P3Sections.forEachIndexed { index, section ->
            if (isCoordinateInsideBox(playerCoords, section.first, section.second)) {
                return index + 1
            }
        }

        return 1
    }

    private val bossRoomCorners = mapOf(
        7 to Pair(Vec3(-8.0, 0.0, -8.0), Vec3(134.0, 254.0, 147.0)),
        6 to Pair(Vec3(-40.0, 51.0, -8.0), Vec3(22.0, 110.0, 134.0)),
        5 to Pair(Vec3(-40.0, 53.0, -8.0), Vec3(50.0, 112.0, 118.0)),
        4 to Pair(Vec3(-40.0, 53.0, -40.0), Vec3(134.0, 254.0, 147.0)),
        3 to Pair(Vec3(-40.0, 0.0, -40.0), Vec3(42.0, 118.0, 73.0))
    )

    private fun isInBossRoom(): Boolean {

        val playerCoords = Player?.positionVector ?: return false
        val corners = bossRoomCorners[dungeonFloor] ?: return false

        return isCoordinateInsideBox(playerCoords, corners.first, corners.second)
    }
}
