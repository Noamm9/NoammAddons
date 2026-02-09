package com.github.noamm9.utils.location

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.*
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.Utils.remove
import com.github.noamm9.utils.Utils.startsWithOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import kotlin.jvm.optionals.getOrNull

object LocationUtils {
    @JvmStatic
    val onHypixel get() = mc.player?.connection?.serverBrand()?.lowercase()?.contains("hypixel") == true

    @JvmField
    var inSkyblock = false

    @JvmField
    var world: WorldType? = null

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

    var lobbyId: String? = null
        private set

    private val lobbyRegex = Regex("\\d\\d/\\d\\d/\\d\\d (\\w{0,6}) *")

    init {
        EventBus.register<MainThreadPacketReceivedEvent.Post>(EventPriority.HIGHEST) {
            if (NoammAddons.debugFlags.contains("dev")) return@register setDevModeValues()
            if (! onHypixel) return@register

            if (event.packet is ClientboundPlayerInfoUpdatePacket) {
                val actions = event.packet.actions()
                if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) || actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                    val area = event.packet.entries().find { it.displayName()?.string?.startsWithOneOf("Area: ", "Dungeon: ") == true }?.displayName?.string ?: return@register
                    world = WorldType.entries.firstOrNull { area.remove("Area: ", "Dungeon: ") == it.tabName }
                }
            }
            else if (event.packet is ClientboundSetPlayerTeamPacket) {
                val prams = event.packet.parameters.getOrNull() ?: return@register
                val text = (prams.playerPrefix.string + prams.playerSuffix.string).removeFormatting()
                lobbyRegex.find(text)?.groupValues?.get(1)?.let { lobbyId = it }

                if (! inDungeon && text.contains("The Catacombs (") && ! text.contains("Queue")) {
                    inDungeon = true
                    dungeonFloor = text.substringAfter("(").substringBefore(")")
                    dungeonFloorNumber = dungeonFloor?.lastOrNull()?.digitToIntOrNull() ?: 0
                }
            }
            else if (event.packet is ClientboundSetObjectivePacket) {
                if (! inSkyblock) inSkyblock = onHypixel && event.packet.objectiveName == "SBScoreboard"
            }
        }

        EventBus.register<TickEvent.Start>(EventPriority.HIGHEST) {
            inBoss = isInBossRoom()
            if (inBoss && DungeonListener.bossEntryTime == null) {
                DungeonListener.bossEntryTime = DungeonListener.currentTime
                EventBus.post(DungeonEvent.BossEnterEvent)
            }
            F7Phase = getPhase()
            P3Section = findP3Section()
        }

        EventBus.register<WorldChangeEvent>(EventPriority.HIGHEST) { reset() }
        EventBus.register<ServerEvent.Disconnect>(EventPriority.HIGHEST) { reset() }
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
        P3Section = findP3Section()
        inBoss = isInBossRoom()
    }

    private fun getPhase(): Int? {
        if (dungeonFloorNumber != 7 || ! inBoss) return null
        val y = mc.player?.y ?: return null

        return when {
            y > 210 -> 1
            y > 155 -> 2
            y > 100 -> 3
            y > 45 -> 4
            else -> 5
        }
    }

    private val P3Sections = arrayOf(
        Pair(BlockPos(90, 158, 123), BlockPos(111, 105, 32)),  //  1
        Pair(BlockPos(16, 158, 122), BlockPos(111, 105, 143)), //  2
        Pair(BlockPos(19, 158, 48), BlockPos(- 3, 106, 142)),  //  3
        Pair(BlockPos(91, 158, 50), BlockPos(- 3, 106, 30))    //  4
    )

    private fun findP3Section(): Int? {
        if (F7Phase != 3) return null
        val playerPos = mc.player?.position() ?: return null

        P3Sections.forEachIndexed { i, (a, b) ->
            if (MathUtils.isCoordinateInsideBox(playerPos, a, b)) {
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
        val playerPos = mc.player?.position() ?: return false
        val floor = dungeonFloorNumber ?: return false
        val corners = bossRoomCorners[floor] ?: return false
        return MathUtils.isCoordinateInsideBox(playerPos, corners.first, corners.second)
    }
}