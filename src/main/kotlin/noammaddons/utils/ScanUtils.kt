package noammaddons.utils

import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3i
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RegisterEvents
import noammaddons.events.WorldUnloadEvent
import noammaddons.features.dungeons.dmap.core.map.RoomData
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.equalsOneOf
import kotlin.math.floor


object ScanUtils {
    val roomList = mutableListOf<RoomData>()
    val roomCache = mutableMapOf<Pair<Int, Int>, RoomData>()

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        roomCache.clear()
        currentRoom = null
        lastKnownRoom = null
    }

    init {
        loop(250) {
            if (! config.DevMode) {
                if (mc.thePlayer == null) return@loop
                if (! inDungeon) return@loop
                if (inBoss) return@loop
            }

            val room = getRoomFromPos(mc.thePlayer.position)
            if (currentRoom == room) return@loop

            lastKnownRoom = currentRoom
            currentRoom = room

            RegisterEvents.checkForRoomChange(currentRoom, lastKnownRoom)
        }
    }

    @JvmField
    var currentRoom: RoomData? = null

    @JvmField
    var lastKnownRoom: RoomData? = null

    init {
        JsonUtils.fetchJsonWithRetry<List<RoomData>?>(
            "https://raw.githubusercontent.com/Skytils/SkytilsMod/refs/heads/1.x/src/main/resources/assets/catlas/rooms.json"
        ) {
            if (it == null) return@fetchJsonWithRetry
            roomList.addAll(it)
        }
    }


    fun getRoomData(x: Int, z: Int): RoomData? {
        return getRoomData(getCore(x, z))
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

    fun getRoomCenterAt(pos: Vec3i): Pair<Int, Int> {
        return getRoomCenter(getRoomCorner(getRoomComponnent(pos)))
    }

    fun getRoomFromPos(pos: BlockPos): RoomData? {
        val comp = getRoomComponnent(pos)
        roomCache[comp]?.let {
            return it
        }

        val (cx, cz) = getRoomCenter(getRoomCorner(comp))
        val room = getRoomData(cx, cz) ?: return null
        roomCache[comp] = room
        return room
    }

    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val chunk = mc.theWorld.getChunkFromBlockCoords(BlockPos(x, 0, z))
        for (y in 140 downTo 12) {
            val id = chunk.getBlock(BlockPos(x, y, z)).getBlockId()
            if (! id.equalsOneOf(5, 54, 146)) {
                sb.append(id)
            }
        }
        return sb.toString().hashCode()
    }

    fun getRotation(center: Pair<Int, Int>, relativeCoords: Map<Block, List<Int>>): Int? {
        var detectedRotation: Int? = null

        relativeCoords.forEach { (block, coords) ->
            for (i in 0 .. 3) {
                val pos = getRealCoord(coords, listOf(center.first, 0, center.second), i * 90)
                if (getBlockAt(pos) != block) continue
                if (detectedRotation == null) detectedRotation = i
                else if (detectedRotation != i) {
                    Utils.printCaller()
                    modMessage("&cConflicting rotations detected")
                    return null
                }
            }
        }

        return detectedRotation
    }

    fun rotateCoords(coords: List<Int>, degree: Int): List<Int> {
        var adjustedDegree = degree
        if (adjustedDegree < 0) adjustedDegree += 360

        return when (adjustedDegree) {
            0 -> listOf(coords[0], coords[1], coords[2])
            90 -> listOf(coords[2], coords[1], - coords[0])
            180 -> listOf(- coords[0], coords[1], - coords[2])
            270 -> listOf(- coords[2], coords[1], coords[0])
            else -> listOf(coords[0], coords[1], coords[2])
        }
    }

    fun getRealCoord(array: List<Int>, roomCenter: List<Int>, rotation: Int): BlockPos {
        val (cx, _, cz) = roomCenter
        val (x, y, z) = rotateCoords(array, rotation)
        if (rotation == 0) return BlockPos(array[0] + cx, array[1], array[2] + cz)

        return BlockPos(cx + x, y, cz + z)
    }
}
