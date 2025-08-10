package noammaddons.features.impl.dungeons.dmap.handlers

import net.minecraft.world.storage.MapData
import noammaddons.features.impl.dungeons.dmap.core.map.*

object DungeonInfo {
    val dungeonList = Array<Tile>(121) { Unknown(0, 0) }
    val uniqueRooms = mutableSetOf<UniqueRoom>()
    var roomCount = 0
    val puzzles = mutableMapOf<Puzzle, Boolean>()

    var trapType = ""
    var witherDoors = 0
    var cryptCount = 0
    var secretCount = 0

    var keys = 0

    var dungeonMap: MapData? = null
    var guessMapData: MapData? = null

    fun reset() {
        dungeonList.fill(Unknown(0, 0))
        roomCount = 0
        uniqueRooms.clear()
        puzzles.clear()

        trapType = ""
        witherDoors = 0
        cryptCount = 0
        secretCount = 0

        keys = 0

        dungeonMap = null
        guessMapData = null
    }
}