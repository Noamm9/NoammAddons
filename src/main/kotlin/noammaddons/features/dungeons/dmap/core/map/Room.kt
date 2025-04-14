package noammaddons.features.dungeons.dmap.core.map

import noammaddons.events.DungeonEvent
import noammaddons.events.RegisterEvents
import noammaddons.features.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.dungeons.dmap.handlers.DungeonScanner
import noammaddons.utils.*
import java.awt.Color
import kotlin.properties.Delegates

class Room(override val x: Int, override val z: Int, var data: RoomData): Tile {
    var core = 0
    var isSeparator = false

    override var state: RoomState by Delegates.observable(RoomState.UNDISCOVERED) { _, oldValue, newValue ->
        if (oldValue == newValue) return@observable
        if (data.name == "Unknown") return@observable
        if (DungeonMapConfig.dungeonMapCheater && oldValue == RoomState.UNOPENED && newValue == RoomState.UNDISCOVERED) return@observable
        if (DungeonMapConfig.dungeonMapCheater && newValue == RoomState.UNOPENED && oldValue == RoomState.UNDISCOVERED) return@observable
        ChatUtils.debugMessage("${data.name}: $oldValue -> $newValue")

        val roomPlayers = DungeonUtils.dungeonTeammates.filter {
            ScanUtils.getRoomFromPos(it.mapIcon.getRealPos())?.name == data.name
        }

        RegisterEvents.postAndCatch(DungeonEvent.RoomEvent.onStateChange(data, oldValue, newValue, roomPlayers))
    }


    override val color: Color
        get() {
            return if (state == RoomState.UNOPENED) DungeonMapConfig.colorUnopened
            else when (data.type) {
                RoomType.BLOOD -> DungeonMapConfig.colorBlood
                RoomType.CHAMPION -> DungeonMapConfig.colorMiniboss
                RoomType.ENTRANCE -> DungeonMapConfig.colorEntrance
                RoomType.FAIRY -> DungeonMapConfig.colorFairy
                RoomType.PUZZLE -> DungeonMapConfig.colorPuzzle
                RoomType.RARE -> DungeonMapConfig.colorRare
                RoomType.TRAP -> DungeonMapConfig.colorTrap
                else -> DungeonMapConfig.colorRoom
            }
        }
    var uniqueRoom: UniqueRoom? = null

    fun getArrayPosition(): Pair<Int, Int> {
        return Pair((x - DungeonScanner.startX) / 16, (z - DungeonScanner.startZ) / 16)
    }

    fun addToUnique(row: Int, column: Int, roomName: String = data.name) {
        val unique = DungeonInfo.uniqueRooms.find { it.name == roomName }

        if (unique == null) {
            UniqueRoom(column, row, this).let {
                DungeonInfo.uniqueRooms.add(it)
                uniqueRoom = it
            }
        }
        else {
            unique.addTile(column, row, this)
            uniqueRoom = unique
        }
    }
}