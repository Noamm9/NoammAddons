package com.github.noamm9.utils.dungeons.map.utils

import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.handlers.HotbarMapColorParser
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.world.level.saveddata.maps.MapDecoration
import net.minecraft.world.phys.Vec3

object MapUtils {
    val MapDecoration.mapX get() = (this.x + 128) shr 1
    val MapDecoration.mapZ get() = (this.y + 128) shr 1
    val MapDecoration.yaw get() = this.rot * 22.5f

    var startCorner = Pair(5, 5)
    var mapRoomSize = 16
    var coordMultiplier = 0.625
    var calibrated = false


    fun coordsToMap(vec: Vec3): Pair<Float, Float> {
        val x = ((vec.x - DungeonScanner.startX + 15) * coordMultiplier + startCorner.first).toFloat()
        val z = ((vec.z - DungeonScanner.startZ + 15) * coordMultiplier + startCorner.second).toFloat()
        return Pair(x, z)
    }

    fun reset() {
        startCorner = Pair(5, 5)
        mapRoomSize = 16
        coordMultiplier = 0.625
        calibrated = false
    }


    fun calibrateMap(): Boolean {
        val (start, size) = findEntranceCorner()
        if (! size.equalsOneOf(16, 18)) return false

        mapRoomSize = size
        startCorner = when (LocationUtils.dungeonFloorNumber) {
            0 -> Pair(22, 22)
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


    private fun findEntranceCorner(): Pair<Int, Int> {
        var start = 0
        var currLength = 0

        DungeonInfo.mapData?.colors?.forEachIndexed { index, byte ->
            if (byte == 30.toByte()) {
                if (currLength == 0) start = index
                currLength ++
            }
            else {
                if (currLength >= 16) return Pair(start, currLength)
                currLength = 0
            }
        }

        return Pair(start, currLength)
    }
}