package noammaddons.features.impl.dungeons.dmap.utils

import net.minecraft.util.Vec3
import net.minecraft.util.Vec4b
import noammaddons.features.impl.dungeons.dmap.handlers.*
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.Utils.equalsOneOf


object MapUtils {
    val Vec4b.mapX get() = ((func_176112_b() + 128) shr 1).toFloat()
    val Vec4b.mapZ get() = ((func_176113_c() + 128) shr 1).toFloat()
    val Vec4b.yaw get() = func_176111_d() * 22.5f

    fun coordsToMap(vec: Vec3): Pair<Float, Float> {
        val x = ((vec.xCoord - DungeonScanner.startX + 15) * coordMultiplier + startCorner.first).toFloat()
        val z = ((vec.zCoord - DungeonScanner.startZ + 15) * coordMultiplier + startCorner.second).toFloat()
        return Pair(x, z)
    }

    var startCorner = Pair(5, 5)
    var mapRoomSize = 16
    var coordMultiplier = 0.625
    var calibrated = false

    /**
     * Calibrates map metrics based on the size and location of the entrance room.
     */
    fun calibrateMap(): Boolean {
        val (start, size) = findEntranceCorner()
        if (size.equalsOneOf(16, 18)) {
            mapRoomSize = size
            startCorner = when (dungeonFloorNumber) {
                null -> Pair(22, 22)
                1 -> Pair(22, 11)
                2, 3 -> Pair(11, 11)
                else -> {
                    val startX = start and 127
                    val startZ = start shr 7
                    Pair(startX % (mapRoomSize + 4), startZ % (mapRoomSize + 4))
                }
            }
            coordMultiplier = (mapRoomSize + 4.0) / DungeonScanner.roomSize

            HotbarMapColorParser.calibrate()
            return true
        }
        return false
    }

    /**
     * Finds the starting index of the entrance room as well as the size of the room.
     */
    private fun findEntranceCorner(): Pair<Int, Int> {
        var start = 0
        var currLength = 0
        DungeonInfo.mapData?.colors?.forEachIndexed { index, byte ->
            if (byte == 30.toByte()) {
                if (currLength == 0) start = index
                currLength ++
            }
            else {
                if (currLength >= 16) {
                    return Pair(start, currLength)
                }
                currLength = 0
            }
        }
        return Pair(start, currLength)
    }
}
