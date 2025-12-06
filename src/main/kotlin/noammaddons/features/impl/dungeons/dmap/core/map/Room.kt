package noammaddons.features.impl.dungeons.dmap.core.map

import net.minecraft.util.BlockPos
import noammaddons.events.DungeonEvent
import noammaddons.events.EventDispatcher
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner.halfRoomSize
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.BlockUtils.getMetadata
import noammaddons.utils.BlockUtils.getStateAt
import noammaddons.utils.MathUtils.add
import java.awt.Color
import kotlin.properties.Delegates

class Room(override val x: Int, override val z: Int, var data: RoomData): Tile {
    var core = 0
    var isSeparator = false
    var rotation: Int? = null
    var corner: BlockPos? = null
    var highestBlock: Int? = null
    var uniqueRoom: UniqueRoom? = null

    override var state: RoomState by Delegates.observable(RoomState.UNDISCOVERED) { _, oldValue, newValue ->
        if (uniqueRoom?.mainRoom != this) return@observable
        if (oldValue == newValue) return@observable
        if (data.name == "Unknown") return@observable
        if (DungeonMapConfig.dungeonMapCheater.value && oldValue == RoomState.UNOPENED && newValue == RoomState.UNDISCOVERED) return@observable
        if (DungeonMapConfig.dungeonMapCheater.value && newValue == RoomState.UNOPENED && oldValue == RoomState.UNDISCOVERED) return@observable
        ChatUtils.debugMessage("${data.name}: $oldValue -> $newValue")

        val roomPlayers = DungeonUtils.dungeonTeammates.filter {
            ScanUtils.getRoomFromPos(it.mapIcon.getRealPos())?.data?.name == data.name
        }

        EventDispatcher.postAndCatch(DungeonEvent.RoomEvent.onStateChange(this, oldValue, newValue, roomPlayers))
    }

    @Suppress("RecursivePropertyAccessor")
    override val color: Color
        get() {
            return if (state == RoomState.UNOPENED) {
                if (DungeonMapConfig.dungeonMapCheater.value) {
                    state = RoomState.UNDISCOVERED
                    return this.color
                }
                DungeonMapConfig.colorUnopened.value
            }
            else when (data.type) {
                RoomType.BLOOD -> DungeonMapConfig.colorBlood
                RoomType.CHAMPION -> DungeonMapConfig.colorMiniboss
                RoomType.ENTRANCE -> DungeonMapConfig.colorEntrance
                RoomType.FAIRY -> DungeonMapConfig.colorFairy
                RoomType.PUZZLE -> DungeonMapConfig.colorPuzzle
                RoomType.RARE -> DungeonMapConfig.colorRare
                RoomType.TRAP -> DungeonMapConfig.colorTrap
                else -> DungeonMapConfig.colorRoom
            }.value
        }

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
            corner = BlockPos(x - 15, 0, z - 15)
            return
        }

        val uniqueRoom = uniqueRoom ?: return
        val yLevel = highestBlock !!.toDouble()
        val scannedPositions = HashSet<BlockPos>()

        val minX = uniqueRoom.tiles.minOf { it.x }
        val maxX = uniqueRoom.tiles.maxOf { it.x }
        val minZ = uniqueRoom.tiles.minOf { it.z }
        val maxZ = uniqueRoom.tiles.maxOf { it.z }

        listOf(
            Pair(minX - halfRoomSize, minZ - halfRoomSize),
            Pair(maxX + halfRoomSize, minZ - halfRoomSize),
            Pair(maxX + halfRoomSize, maxZ + halfRoomSize),
            Pair(minX - halfRoomSize, maxZ + halfRoomSize)
        ).forEachIndexed { i, (x, z) ->
            val pos = BlockPos(x, yLevel, z)
            scannedPositions.add(pos)
            getStateAt(pos).takeIf { it.getBlockId() == 159 && it.getMetadata() == 11 }?.let {
                rotation = i * 90
                corner = pos.add(y = - yLevel)
                return
            }
        }

        for ((x, z) in uniqueRoom.tiles.map { Pair(it.x, it.z) }) {
            DungeonScanner.clayBlocksCorners.forEachIndexed { i, (rx, rz) ->
                val pos = BlockPos(x + rx, yLevel, z + rz)
                if (scannedPositions.contains(pos)) return@forEachIndexed
                getStateAt(pos).takeIf { it.getBlockId() == 159 && it.getMetadata() == 11 }?.let {
                    rotation = i * 90
                    corner = pos.add(y = - yLevel)
                    return
                }
            }
        }
    }
}