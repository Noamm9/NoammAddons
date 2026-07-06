package com.github.noamm9.utils.dungeons.map

import com.github.noamm9.utils.dungeons.map.core.Tile
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.core.Unknown
import net.minecraft.world.level.saveddata.maps.MapItemSavedData

object DungeonInfo {
    val dungeonList = Array<Tile>(121) { Unknown(0, 0) }
    val uniqueRooms = mutableMapOf<String, UniqueRoom>()

    var mimicRoom: UniqueRoom? = null
    var witherDoors = 0
    var cryptCount = 0
    var secretCount = 0

    var mapData: MapItemSavedData? = null

    fun reset() {
        dungeonList.fill(Unknown(0, 0))
        uniqueRooms.clear()

        witherDoors = 0
        cryptCount = 0
        secretCount = 0

        mapData = null
    }
}