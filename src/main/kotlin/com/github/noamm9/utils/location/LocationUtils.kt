package com.github.noamm9.utils.location

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Room
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.startsWithOneOf
import com.github.noamm9.websocket.WebSocket
import com.github.noamm9.websocket.packets.C2SPacketDungeonStart
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.world.phys.AABB
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

    @JvmField
    var serverId: String? = null

    private val lobbyRegex = Regex("\\d\\d/\\d\\d/\\d\\d (\\w{0,6}) *")

    init {
        LocrawListener.init()

        EventBus.register<MainThreadPacketReceivedEvent.Post>(EventPriority.HIGHEST) {
            if (NoammAddons.isDev) return@register setDevModeValues()
            if (! onHypixel) return@register

            if (event.packet is ClientboundPlayerInfoUpdatePacket) {
                val actions = event.packet.actions()
                if (actions.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) || actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                    val area = event.packet.entries().find { it.displayName?.string?.startsWithOneOf("Area: ", "Dungeon: ") == true }?.displayName?.string ?: return@register
                    world = WorldType.entries.firstOrNull { area.remove("Area: ", "Dungeon: ") == it.tabName }
                }
            }
            else if (event.packet is ClientboundSetPlayerTeamPacket) {
                val prams = event.packet.parameters.getOrNull() ?: return@register
                val text = (prams.playerPrefix.string + prams.playerSuffix.string).removeFormatting()
                lobbyRegex.find(text)?.groupValues?.get(1)?.let { serverId = it }

                if (! inDungeon && text.contains("The Catacombs (") && ! text.contains("Queue")) {
                    inDungeon = true
                    dungeonFloor = text.substringAfter("(").substringBefore(")")
                    dungeonFloorNumber = dungeonFloor?.lastOrNull()?.digitToIntOrNull() ?: 0

                    ThreadUtils.scheduledTaskServer(30) ws@{
                        if (DungeonListener.dungeonTeammatesNoSelf.isEmpty()) return@ws
                        val serverId = LocrawListener.server.ifEmpty { serverId } ?: return@ws
                        val floor = dungeonFloor ?: return@ws
                        val team = DungeonListener.dungeonTeammates.map { it.name }.ifEmpty { return@ws }
                        val entrance = (DungeonInfo.dungeonList.find { (it as? Room)?.data?.type == RoomType.ENTRANCE } as? Room)?.getArrayPosition() ?: return@ws

                        WebSocket.send(C2SPacketDungeonStart(serverId, floor, team, entrance))
                    }
                }
            }
            else if (event.packet is ClientboundSetObjectivePacket) {
                if (! inSkyblock) inSkyblock = onHypixel && event.packet.objectiveName == "SBScoreboard"
            }
        }

        EventBus.register<TickEvent.Start>(EventPriority.HIGHEST) {
            inBoss = isInBossRoom()
            if (inBoss && DungeonListener.bossEntryTime == null) {
                DungeonListener.bossEntryTime = DungeonListener.DualTime(DungeonListener.currentTime)
                EventBus.post(DungeonEvent.BossEnterEvent)
            }
            F7Phase = getPhase()
            P3Section = findP3Section()
        }

        EventBus.register<WorldChangeEvent>(EventPriority.HIGHEST) { reset() }
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
        serverId = null
        WebSocket.send(mapOf("type" to "reset"))
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

    private val p3Sections = arrayOf(
        AABB(90.0, 158.0, 123.0, 111.0, 105.0, 32.0),
        AABB(16.0, 158.0, 122.0, 111.0, 105.0, 143.0),
        AABB(19.0, 158.0, 48.0, - 3.0, 106.0, 142.0),
        AABB(91.0, 158.0, 50.0, - 3.0, 106.0, 30.0)
    )

    private fun findP3Section(): Int? {
        if (F7Phase != 3) return null
        val playerPos = mc.player?.position() ?: return null

        for (i in p3Sections.indices) {
            if (p3Sections[i].contains(playerPos)) {
                return i + 1
            }
        }

        return null
    }

    private val bossRoomBounds = arrayOf(
        AABB(- 14.0, 55.0, 49.0, - 72.0, 146.0, - 40.0),
        AABB(- 40.0, 99.0, - 40.0, 24.0, 54.0, 59.0),
        AABB(- 40.0, 118.0, - 40.0, 42.0, 64.0, 37.0),
        AABB(- 40.0, 112.0, - 40.0, 50.0, 53.0, 47.0),
        AABB(- 40.0, 112.0, - 8.0, 50.0, 53.0, 118.0),
        AABB(- 40.0, 51.0, - 8.0, 22.0, 110.0, 134.0),
        AABB(- 8.0, 0.0, - 8.0, 134.0, 254.0, 147.0)
    )

    private fun isInBossRoom(): Boolean {
        val playerPos = mc.player?.position() ?: return false
        val floor = dungeonFloorNumber ?: return false
        if (floor !in 1 .. 7) return false
        return bossRoomBounds[floor - 1].contains(playerPos)
    }
}