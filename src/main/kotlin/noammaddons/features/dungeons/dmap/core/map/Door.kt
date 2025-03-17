package noammaddons.features.dungeons.dmap.core.map

import noammaddons.features.dungeons.dmap.core.DungeonMapConfig
import java.awt.Color

class Door(override val x: Int, override val z: Int, var type: DoorType): Tile {
    var opened = false
    override var state: RoomState = RoomState.UNDISCOVERED
    override val color: Color
        get() {
            return if (state == RoomState.UNOPENED) DungeonMapConfig.colorUnopenedDoor
            else when (this.type) {
                DoorType.BLOOD -> DungeonMapConfig.colorBloodDoor
                DoorType.ENTRANCE -> DungeonMapConfig.colorEntranceDoor
                DoorType.WITHER -> if (opened) DungeonMapConfig.colorOpenWitherDoor else DungeonMapConfig.colorWitherDoor
                else -> DungeonMapConfig.colorRoomDoor
            }
        }
}
