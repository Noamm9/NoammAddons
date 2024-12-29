package noammaddons.features.dungeons

import net.minecraft.util.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noammaddons.events.Tick
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ScanUtils.ScanRoom.currentRoom
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.walker
import kotlin.math.floor

object AutoIceFill {
    private var walking = true
    private var path = mutableListOf<Pair<Int, Int>>()
    private var pattern: List<List<Int>>? = null
    private var TickOn = true
    private var KeyInputOn = true

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onTick(event: Tick) {
        if (! TickOn) return


        val blockId = getBlockAt(
            BlockPos(
                floor(Player !!.posX).toInt(),
                floor(Player !!.posY - 1).toInt(),
                floor(Player !!.posZ).toInt()
            )
        )?.getBlockId() ?: return

        if (blockId == 79) walking = true // Ice block

        if (walking) {
            if (! walker.walk(path)) return
            walking = false
        }
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (! KeyInputOn) return
        if (walking) walker.updateKeys()
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onStep(event: Tick) {
        if (inDungeons && currentRoom?.name == "Ice Fill") {
            pattern = Solver.solve() ?: return
            path = pattern !!.map { Pair(it[0], it[2]) }.toMutableList()
            TickOn = true
            KeyInputOn = true
        }
        else {
            pattern = null
            path.clear()
            TickOn = false
            KeyInputOn = false
        }
    }
}


object Solver {
    val start = listOf(listOf(15, 7), listOf(23, 15), listOf(15, 23), listOf(7, 15))
    private val representativeFloors = listOf(
        listOf(listOf(0, - 1, 2, - 1), listOf(0, 1, 2, 1), listOf(2, 1, 0, 1), listOf(2, - 1, 0, - 1)),
        listOf(listOf(1, 0, 1, - 1), listOf(2, 1, 2, 0), listOf(2, 1, 1, - 1), listOf(0, - 2, 1, 0), listOf(3, 0, 0, - 2), listOf(2, - 1, 2, 0)),
        listOf(listOf(4, 2, 4, 1), listOf(3, - 1, 2, - 1), listOf(2, 3, 2, 2), listOf(3, 0, 3, - 1), listOf(3, 2, 3, 1))
    )
    private val floors = listOf(
        listOf(listOf(0, 0, - 1), listOf(1, 0, - 1), listOf(1, 0, 0), listOf(1, 0, 1), listOf(2, 0, 1), listOf(2, 0, 0), listOf(3, 0, 0)),
        listOf(listOf(0, 0, 1), listOf(1, 0, 1), listOf(1, 0, 0), listOf(1, 0, - 1), listOf(2, 0, - 1), listOf(2, 0, 0), listOf(3, 0, 0)),
        listOf(listOf(1, 0, 0), listOf(1, 0, 1), listOf(2, 0, 1), listOf(2, 0, 0), listOf(3, 0, 0)),
        listOf(listOf(1, 0, 0), listOf(1, 0, - 1), listOf(2, 0, - 1), listOf(2, 0, 0), listOf(3, 0, 0))
    )

    private fun scanAllFloors(pos: List<Int>, rotation: Int): List<List<Int>> {
        val fullPattern = mutableListOf<List<Int>>()
        val starts = listOf(
            pos,
            addPositions(pos, transformTo(listOf(5, 1, 0), rotation)),
            addPositions(pos, transformTo(listOf(12, 2, 0), rotation))
        )
        for (i in starts) {
            var pattern = scan(i, starts.indexOf(i), rotation).toMutableList()
            pattern.add(0, listOf(0, 0, 0))
            pattern.add(addPositions(pattern.last(), listOf(1, 1, 0)))
            pattern = pattern.map { addPositions(transformTo(it, rotation), i) }.toMutableList()
            fullPattern.addAll(pattern)
        }
        return fullPattern
    }

    private fun scan(pos: List<Int>, floorIndex: Int, rotation: Int): List<List<Int>> {
        val floorHeight = representativeFloors[floorIndex]
        for (i in floorHeight.indices) {
            val p1 = transform(floorHeight[i][0], floorHeight[i][1], rotation).toMutableList().apply { add(1, 0) }
            val p2 = transform(floorHeight[i][2], floorHeight[i][3], rotation).toMutableList().apply { add(1, 0) }
            if (isAir(addPositions(pos, p1)) && ! isAir(addPositions(pos, p2))) {
                return listOf(floors[floorIndex][i])
            }
        }
        return listOf()
    }

    private fun transform(x: Int, z: Int, rotation: Int): List<Int> {
        return when (rotation) {
            0 -> listOf(- z, x)
            1 -> listOf(- x, - z)
            2 -> listOf(z, - x)
            else -> listOf(x, z)
        }
    }

    private fun transformTo(vec: List<Int>, rotation: Int): List<Int> {
        return when (rotation) {
            0 -> listOf(- vec[2], vec[1], vec[0])
            1 -> listOf(- vec[0], vec[1], - vec[2])
            2 -> listOf(vec[2], vec[1], - vec[0])
            else -> listOf(vec[0], vec[1], vec[2])
        }
    }

    private fun addPositions(pos1: List<Int>, pos2: List<Int>): List<Int> {
        return pos1.mapIndexed { index, value -> value + pos2[index] }
    }

    private fun isAir(pos: List<Int>): Boolean {
        val world: World = mc.theWorld
        val block = world.getBlockState(BlockPos(pos[0], pos[1], pos[2])).block
        return block.getBlockId() == 0
    }

    fun solve(): List<List<Int>>? {
        val player = Player ?: return null
        val offsetX = (floor((player.posX + 200) / 32) * 32).toInt() - 200
        val offsetZ = (floor((player.posZ + 200) / 32) * 32).toInt() - 200

        for (i in start.indices) {
            val (x, z) = start[i]
            val (fx, fz) = listOf(offsetX + x, offsetZ + z)
            val block = getBlockAt(BlockPos(fx, 69, fz))
            if (! block?.getBlockId().equalsOneOf(174, 79)) continue
            return scanAllFloors(listOf(fx, 70, fz), i)
        }
        return null
    }

}