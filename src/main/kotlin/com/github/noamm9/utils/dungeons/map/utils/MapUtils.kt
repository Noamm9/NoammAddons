package com.github.noamm9.utils.dungeons.map.utils

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.handlers.HotbarMapScanner
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import net.minecraft.world.phys.Vec3

object MapUtils: ISelfInit {
    var startCorner = Pair(5, 5)
    var mapRoomSize = 16
    var coordMultiplier = 0.625
    var calibrated = false

    override fun init() {
        EventBus.register<WorldChangeEvent> { reset() }
    }

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

    fun calibrateMap(mapData: MapItemSavedData): Boolean {
        if (calibrated) return true

        val (start, size) = findEntranceCorner(mapData)
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

        HotbarMapScanner.calibrate()
        return true
    }


    private fun findEntranceCorner(mapData: MapItemSavedData): Pair<Int, Int> {
        var currLength = 0
        var start = 0

        mapData.colors.forEachIndexed { index, byte ->
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