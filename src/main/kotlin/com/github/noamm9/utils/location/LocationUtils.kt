package com.github.noamm9.utils.location

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Shortcuts
import com.github.noamm9.features.impl.dev.FEAT_WebSocket
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.mixin.IServerboundMovePlayerPacket
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.startsWithOneOf
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import kotlin.jvm.optionals.getOrNull

object LocationUtils: ISelfInit, Shortcuts {
    @JvmStatic val onHypixel get() = mc.player?.connection?.serverBrand()?.lowercase()?.contains("hypixel") == true

    @JvmField var inSkyblock = false
    @JvmField var world: WorldType? = null
    @JvmField var serverId: String? = null

    @JvmField var inDungeon = false
    @JvmField var dungeonFloor: String? = null
    @JvmField var dungeonFloorNumber: Int? = null
    @JvmStatic val isMasterMode get() = dungeonFloor?.startsWith("M") == true

    @JvmField var inBoss = false
    @JvmField var P3Section: Int? = null
    @JvmField var F7Phase: Int? = null

    override fun init() {
        EventBus.register<MainThreadPacketReceivedEvent.Post>(EventPriority.HIGHEST) {
            if ("dev" in NoammAddons.debugFlags) return@register setDevModeValues()
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

                    FEAT_WebSocket.sendDungeonInfo()
                }
            }
            else if (event.packet is ClientboundSetObjectivePacket) {
                if (! inSkyblock) inSkyblock = onHypixel && event.packet.objectiveName == "SBScoreboard"
            }
        }

        EventBus.register<PacketEvent.Sent>(EventPriority.HIGH) {
            if (event.packet !is ServerboundMovePlayerPacket) return@register
            if (! event.packet.hasPosition()) return@register
            val packet = event.packet as IServerboundMovePlayerPacket

            inBoss = isInBossRoom(packet.x, packet.y, packet.z)
            if (inBoss && DungeonListener.bossEntryTime == null) {
                DungeonListener.bossEntryTime = DungeonListener.DualTime(DungeonListener.currentTime)
                EventBus.post(DungeonEvent.BossEnterEvent)
            }
            F7Phase = getPhase(packet.y)
            P3Section = findP3Section(packet.x, packet.y, packet.z)
        }

        EventBus.register<WorldChangeEvent>(EventPriority.HIGH) { reset() }
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
    }

    private fun setDevModeValues() {
        if (mc.level == null || mc.player == null) return
        inSkyblock = true
        inDungeon = true
        dungeonFloor = "F7"
        dungeonFloorNumber = 7
        F7Phase = getPhase(player.y)
        P3Section = findP3Section(player.x, player.y, player.z)
        inBoss = isInBossRoom(player.x, player.y, player.z)
    }

    fun getPhase(y: Double): Int? {
        if (dungeonFloorNumber != 7 || ! inBoss) return null

        return when {
            y > 210 -> 1
            y > 155 -> 2
            y > 100 -> 3
            y > 45 -> 4
            else -> 5
        }
    }

    fun findP3Section(x: Double, y: Double, z: Double): Int? {
        if (F7Phase != 3) return null

        for (i in p3Sections.indices) {
            if (p3Sections[i].contains(x, y, z)) {
                return i + 1
            }
        }

        return null
    }

    fun isInBossRoom(x: Double, y: Double, z: Double): Boolean {
        val floor = dungeonFloorNumber?.takeIf { it in 1 .. 7 } ?: return false
        return bossRoomBounds[floor - 1].contains(x, y, z)
    }

    private val lobbyRegex = Regex("\\d\\d/\\d\\d/\\d\\d (\\w{0,6}) *")

    private val p3Sections = arrayOf(
        aabb(90, 158, 123, 111, 105, 32),
        aabb(16, 158, 122, 111, 105, 143),
        aabb(19, 158, 48, - 3, 106, 142),
        aabb(91, 158, 50, - 3, 106, 30)
    )

    private val bossRoomBounds = arrayOf(
        aabb(- 14, 55, 49, - 72, 146, - 40),
        aabb(- 40, 99, - 40, 24, 54, 59),
        aabb(- 40, 118, - 40, 42, 64, 37),
        aabb(- 40, 112, - 40, 50, 53, 47),
        aabb(- 40, 112, - 8, 50, 53, 118),
        aabb(- 40, 51, - 8, 22, 110, 134),
        aabb(- 8, 0, - 8, 134, 254, 147)
    )
}