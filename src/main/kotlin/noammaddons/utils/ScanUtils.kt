package noammaddons.utils

import net.minecraft.block.Block
import net.minecraft.init.Blocks.*
import net.minecraft.util.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.EventDispatcher
import noammaddons.events.WorldUnloadEvent
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.dungeons.dmap.core.map.Room
import noammaddons.features.impl.dungeons.dmap.core.map.RoomData
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.equalsOneOf
import kotlin.math.floor


object ScanUtils {
    val roomList = mutableListOf<RoomData>()

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        currentRoom = null
        lastKnownRoom = null
    }

    init {
        loop(250) {
            if (! DevOptions.devMode) {
                if (mc.thePlayer == null) return@loop
                if (! inDungeon) return@loop
                if (inBoss) return@loop
            }

            val room = getRoomFromPos(mc.thePlayer.position)
            if (currentRoom == room) return@loop

            lastKnownRoom = currentRoom
            currentRoom = room

            EventDispatcher.checkForRoomChange(currentRoom, lastKnownRoom)
        }
    }

    @JvmField
    var currentRoom: Room? = null

    @JvmField
    var lastKnownRoom: Room? = null

    init {
        WebUtils.fetchJsonWithRetry<List<RoomData>>(
            "https://raw.githubusercontent.com/Skytils/SkytilsMod/refs/heads/1.x/src/main/resources/assets/catlas/rooms.json",
            roomList::addAll
        )
    }


    fun getRoomData(hash: Int): RoomData? {
        return roomList.find { hash in it.cores }
    }

    fun getRoomComponnent(pos: Vec3i): Pair<Int, Int> {
        val gx = floor((pos.x + 200 + 0.5) / 32).toInt()
        val gz = floor((pos.z + 200 + 0.5) / 32).toInt()
        return Pair(gx, gz)
    }


    fun getRoomCorner(pair: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(
            - 200 + pair.first * 32,
            - 200 + pair.second * 32
        )
    }

    fun getRoomCenter(pair: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(pair.first + 15, pair.second + 15)
    }

    fun getRoomCenterAt(pos: Vec3i): BlockPos {
        return getRoomCenter(getRoomCorner(getRoomComponnent(pos))).let {
            BlockPos(it.first, 0, it.second)
        }
    }

    fun getRoomFromPos(pos: BlockPos) = DungeonInfo.dungeonList.filterIsInstance<Room>().find { room ->
        room.getRoomComponent() == getRoomComponnent(pos)
    }?.uniqueRoom?.mainRoom


    fun getRoomFromPos(pos: Vec3) = getRoomFromPos(BlockPos(pos))

    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val chunk = mc.theWorld.getChunkFromBlockCoords(BlockPos(x, 0, z))
        for (y in 140 downTo 12) {
            val id = chunk.getBlock(BlockPos(x, y, z)).getBlockId()
            if (id.equalsOneOf(5, 54, 146)) continue
            sb.append(id)
        }
        return sb.toString().hashCode()
    }

    fun getRotation(center: BlockPos, relativeCoords: Map<Block, BlockPos>): Int? {
        relativeCoords.forEach { (block, coords) ->
            for (i in 0 .. 3) {
                val pos = getRealCoord(coords, center, i * 90)
                if (getBlockAt(pos) == block) {
                    return i * 90
                }
            }
        }
        return null
    }

    fun BlockPos.rotate(degree: Int): BlockPos {
        return when ((degree % 360 + 360) % 360) {
            0 -> BlockPos(x, y, z)
            90 -> BlockPos(z, y, - x)
            180 -> BlockPos(- x, y, - z)
            270 -> BlockPos(- z, y, x)
            else -> BlockPos(x, y, z)
        }
    }


    fun getRealCoord(pos: BlockPos, roomCenter: BlockPos, rotation: Int): BlockPos {
        val (cx, _, cz) = roomCenter.destructured()
        return pos.rotate(rotation).add(cx, 0, cz)
    }

    fun gethighestBlockAt(x: Int, z: Int): Int? {
        for (y in 255 downTo 0) {
            val checkPos = BlockPos(x, y, z)
            val block = getBlockAt(checkPos)
            if (block.equalsOneOf(air, wool)) continue
            return checkPos.y
        }
        return null
    }
}
