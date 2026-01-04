package noammaddons.utils

import gg.essential.api.EssentialAPI
import net.minecraft.network.play.server.*
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent
import noammaddons.events.*
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.hud.RunSplits
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.isCoordinateInsideBox
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.startsWithOneOf


object LocationUtils {
    @JvmStatic
    val onHypixel get() = EssentialAPI.getMinecraftUtil().isHypixel()

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

    enum class WorldType(val tabName: String) {
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
    }

    @SubscribeEvent
    fun onPacketRecived(event: MainThreadPacketRecivedEvent.Post) {
        if (DevOptions.devMode) return setDevModeValues()
        if (! onHypixel) return

        if (event.packet is S38PacketPlayerListItem) {
            if (! event.packet.action.equalsOneOf(S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, S38PacketPlayerListItem.Action.ADD_PLAYER)) return
            val area = event.packet.entries?.find { it?.displayName?.noFormatText?.startsWithOneOf("Area: ", "Dungeon: ") == true }?.displayName?.noFormatText ?: return
            world = WorldType.entries.firstOrNull { area.remove("Area: ", "Dungeon: ") == it.tabName }
        }
        else if (event.packet is S3EPacketTeams) {
            if (! event.packet.action.equalsOneOf(0, 2)) return
            val text = event.packet.prefix?.plus(event.packet.suffix)?.removeFormatting() ?: return

            if (! inDungeon && text.contains("The Catacombs (") && ! text.contains("Queue")) {
                inDungeon = true
                dungeonFloor = text.substringAfter("(").substringBefore(")")
                dungeonFloorNumber = dungeonFloor?.lastOrNull()?.digitToIntOrNull() ?: 0
            }
        }
        else if (event.packet is S3BPacketScoreboardObjective) {
            if (! inSkyblock) inSkyblock = onHypixel && event.packet.func_149339_c() == "SBScoreboard"
        }
        else if (event.packet is S32PacketConfirmTransaction) {
            inBoss = isInBossRoom()
            if (inBoss) {
                if (DungeonUtils.bossEntryTime == null) {
                    EventDispatcher.postAndCatch(DungeonEvent.BossEnterEvent())
                    DungeonUtils.bossEntryTime = RunSplits.currentTime
                }
            }
            F7Phase = getPhase()
            P3Section = getP3Section_()
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = reset()

    @SubscribeEvent
    fun onDisconnect(event: ClientDisconnectionFromServerEvent) = reset()

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

    fun isInHubCarnival(): Boolean {
        return isCoordinateInsideBox(
            ServerPlayer.player.getPos() ?: return false,
            BlockPos(- 123, 100, 36), BlockPos(- 64, 70, - 31)
        ) && world == WorldType.Hub
    }

    private fun getPhase(): Int? {
        if (dungeonFloorNumber != 7 || ! inBoss) return null
        val playerPosition = ServerPlayer.player.getPos() ?: return null

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
        val playerPos = ServerPlayer.player.getPos() ?: return null

        P3Sections.forEachIndexed { i, (a, b) ->
            if (isCoordinateInsideBox(playerPos, a, b)) {
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
        val playerPos = ServerPlayer.player.getPos() ?: return false
        val floor = dungeonFloorNumber ?: return false
        val corners = bossRoomCorners[floor] ?: return false
        return isCoordinateInsideBox(playerPos, corners.first, corners.second)
    }
}
