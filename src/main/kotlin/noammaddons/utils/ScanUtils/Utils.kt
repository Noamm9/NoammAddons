package noammaddons.utils.ScanUtils

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ScanUtils.ScanRoom.getRoomCenter
import noammaddons.utils.Utils.equalsOneOf

object Utils {
    /**
     * A collection of coordinates representing the offsets of wither doors in a dungeon 1x1 room.
     * Each offset is represented as an array of two numbers: [x, z].
     */
    private val WitherDoorsOffsets = listOf(
        listOf(0, - 16),
        listOf(16, 0),
        listOf(0, 16),
        listOf(- 16, 0)
    )


    /**
     * Rotates coordinates by a given degree.
     *
     * @param coords Array of 3 doubles representing [x, y, z] coordinates.
     * @param degree Rotation degree in degrees.
     *
     * @return Array of rotated coordinates [x, y, z].
     */
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


    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val chunk = mc.theWorld.getChunkFromChunkCoords(x shr 4, z shr 4)
        val height = chunk.getHeightValue(x and 15, z and 15).coerceIn(11 .. 140)
        sb.append(CharArray(140 - height) { '0' })
        var bedrock = 0
        for (y in height downTo 12) {
            val id = chunk.getBlock(BlockPos(x, y, z)).getBlockId()
            if (id == 0 && bedrock >= 2 && y < 69) {
                sb.append(CharArray(y - 11) { '0' })
                break
            }

            if (id == 7) {
                bedrock ++
            }
            else {
                bedrock = 0
                if (id.equalsOneOf(5, 54, 146)) continue
            }

            sb.append(id)
        }
        return sb.toString().hashCode()
    }

    /**
     * Converts real coordinates to room coordinates.
     *
     * @param x The X coordinate in the real world.
     * @param y The Y coordinate in the real world.
     * @param z The Z coordinate in the real world.
     * @param roomX The X coordinate of the room.
     * @param roomZ The Z coordinate of the room.
     * @param roomRotation The rotation of the room in degrees.
     *
     * @return Array containing the converted room coordinates [rx, ry, rz].
     */
    fun convertToRealCoords(
        x: Int, y: Int, z: Int,
        roomX: Int, roomZ: Int,
        roomRotation: Int
    ): List<Int> {
        val (rx, ry, rz) = rotateCoords(listOf(x, y, z), roomRotation)
        return listOf(rx + roomX, ry, rz + roomZ)
    }

    fun getRealCoord(array: List<Int>, rotation: Int): BlockPos {
        val (cx, cz) = getRoomCenter(Player !!.posX.toInt(), Player !!.posZ.toInt())
        val (dx, dy, dz) = rotateCoords(array, rotation)
        return BlockPos(cx + dx, dy, cz + dz)
    }

    fun getPuzzleRotation(): Int? {
        val roomCenter = getRoomCenter()
        var rotation: Int? = null

        for (i in WitherDoorsOffsets.indices) {
            val (dx, dz) = WitherDoorsOffsets[i]
            val rx = roomCenter.x + dx
            val rz = roomCenter.z + dz

            val block = getBlockAt(BlockPos(rx, 68, rz))
            val bottomBlock = getBlockAt(BlockPos(rx, 67, rz))
            val topBlock = getBlockAt(BlockPos(rx, 69, rz))

            if (bottomBlock != Blocks.bedrock || topBlock != Blocks.air) continue
            if (block == Blocks.air) continue

            if (rotation != null) return rotation
            rotation = i * 90
        }

        return rotation
    }

}