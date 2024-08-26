package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.MathUtils.isCoordinateInsideBox
import NoammAddons.utils.ScoreboardUtils.sidebarLines
import NoammAddons.utils.TablistUtils.getTablistText
import net.minecraft.util.StringUtils.stripControlCodes
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

object LocationUtils {
    var onHypixel = false
    var inSkyblock = false
    var inDungeons = false
    var dungeonFloor = -1
    var inBoss = false
    var P3Section = 1
    var F7Phase = 1
    var WorldName = "hi"

    private var tickCount = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.theWorld == null) return
        tickCount++
        if (tickCount % 20 != 0) return

        if (config.forceSkyblock) {
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
        }
        tickCount = 0
    }

    @SubscribeEvent
    fun onConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        onHypixel = mc.runCatching {
            !event.isLocal && ((thePlayer?.clientBrand?.lowercase()?.contains("hypixel")
                ?: currentServerData?.serverIP?.lowercase()?.contains("hypixel")) == true)
        }.getOrDefault(false)
    }


    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        inDungeons = false
        dungeonFloor = -1
        inBoss = false
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        onHypixel = false
        inSkyblock = false
        inDungeons = false
        dungeonFloor = -1
        inBoss = false
    }

    @SubscribeEvent
    fun onWorldTick(event: TickEvent.WorldTickEvent) {
        if (event.phase != TickEvent.Phase.START || !inSkyblock || !inDungeons) return
        if (!config.forceSkyblock) {
            inBoss = this.isInBossRoom()
            F7Phase = this.getPhase() ?: 1
            P3Section = this.getP3Section() ?: 1
            WorldName = this.getCurrentWorld()
        }
    }


    private fun getPhase(): Int? {
        if (dungeonFloor != 7 && !inBoss) return null

        val player = mc.thePlayer ?: return null
        val corner1 = Vec3(-8.0, 254.0, 147.0)
        val corner2 = Vec3(134.0, 0.0, -8.0)
        var inPhase: Int? = null

        val playerPosition = Vec3(player.posX, player.posY, player.posZ)

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

    private fun getP3Section(): Int? {
        if (getPhase() != 3) return 1

        val player = mc.thePlayer ?: return null
        val playerCoords = Vec3(player.posX, player.posY, player.posZ)

        P3Sections.forEachIndexed { index, section ->
            if (isCoordinateInsideBox(playerCoords, section.first, section.second)) {
                return index + 1
            }
        }

        return 1
    }

    private val bossRoomCorners = mapOf(
        "7" to Pair(Vec3(-8.0, 0.0, -8.0), Vec3(134.0, 254.0, 147.0)),
        "6" to Pair(Vec3(-40.0, 51.0, -8.0), Vec3(22.0, 110.0, 134.0)),
        "5" to Pair(Vec3(-40.0, 53.0, -8.0), Vec3(50.0, 112.0, 118.0)),
        "4" to Pair(Vec3(-40.0, 53.0, -40.0), Vec3(134.0, 254.0, 147.0)),
        "3" to Pair(Vec3(-40.0, 0.0, -40.0), Vec3(42.0, 118.0, 73.0))
    )

    private fun isInBossRoom(): Boolean {
        if (dungeonFloor != 7) return false

        val player = mc.thePlayer ?: return false
        val playerCoords = Vec3(player.posX, player.posY, player.posZ)
        val corners = bossRoomCorners[dungeonFloor.toString()]

        return if (corners != null) isCoordinateInsideBox(playerCoords, corners.first, corners.second)
        else false
    }

    private fun getCurrentWorld(): String {
        if (!inSkyblock) return ""

        for (text in getTablistText()) {
            val worldName = "^(Area|Dungeon): ([\\w ]+)$".toRegex().find(stripControlCodes(text))?.groupValues?.get(2)
            if (!worldName.toBoolean()) continue

            return worldName.toString()
        }

        return ""
    }
}
