package noammaddons.features.impl.dungeons.dmap.core.map

import net.minecraft.util.BlockPos
import noammaddons.events.DungeonEvent
import noammaddons.events.EventDispatcher
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.BlockUtils.getMetadata
import noammaddons.utils.BlockUtils.getStateAt
import java.awt.Color
import kotlin.properties.Delegates

class Room(override val x: Int, override val z: Int, var data: RoomData): Tile {
    var core = 0
    var isSeparator = false
    var rotation: Int? = null
    var highestBlock: Int? = null

    override var state: RoomState by Delegates.observable(RoomState.UNDISCOVERED) { _, oldValue, newValue ->
        if (uniqueRoom?.mainRoom != this) return@observable
        if (oldValue == newValue) return@observable
        if (data.name == "Unknown") return@observable
        if (DungeonMapConfig.dungeonMapCheater && oldValue == RoomState.UNOPENED && newValue == RoomState.UNDISCOVERED) return@observable
        if (DungeonMapConfig.dungeonMapCheater && newValue == RoomState.UNOPENED && oldValue == RoomState.UNDISCOVERED) return@observable
        ChatUtils.debugMessage("${data.name}: $oldValue -> $newValue")

        val roomPlayers = DungeonUtils.dungeonTeammates.filter {
            ScanUtils.getRoomFromPos(it.mapIcon.getRealPos())?.data?.name == data.name
        }

        EventDispatcher.postAndCatch(DungeonEvent.RoomEvent.onStateChange(this, oldValue, newValue, roomPlayers))
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

    fun getRoomComponent(): Pair<Int, Int> = ScanUtils.getRoomComponnent(BlockPos(x, 0, z))

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

    fun findRotation() {
        if (rotation != null) return
        if (highestBlock == null) {
            highestBlock = ScanUtils.gethighestBlockAt(x, z)
            return
        }
        if (data.type == RoomType.FAIRY) {
            rotation = 0
            return
        }

        val realComponents = uniqueRoom?.tiles?.map { Pair(it.x, it.z) } ?: return
        for (c in realComponents) {
            val (x, z) = c
            DungeonScanner.clayBlocksCorners.withIndex().forEach { (i, offset) ->
                val (rx, rz) = offset
                val pos = BlockPos(x + rx, highestBlock !!.toDouble(), z + rz)
                val state = getStateAt(pos)

                if (state.getBlockId() != 159 || state.getMetadata() != 11) return@forEach
                this.rotation = i * 90
                return
            }
        }
    }
}