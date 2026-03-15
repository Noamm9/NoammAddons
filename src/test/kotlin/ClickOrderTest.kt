import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

object ClickOrderTest {
    val type = TerminalType.entries.random()
    private val X = HumanClickOrder.getX(type)
    private val Y = HumanClickOrder.getY(type)
    private val size = X * Y

    @JvmStatic
    fun main(args: Array<String>) {
        val rawSlots = getRandomList()
        val remaining = rawSlots.map { TerminalClick(it) }.toMutableList()
        val finalPath = LinkedList<Int>()

        while (remaining.isNotEmpty()) {
            val bestNextClick = HumanClickOrder.getBestClick(remaining, type)
            finalPath.add(bestNextClick.slotId)
            remaining.remove(bestNextClick)
        }

        printDetailedOutput(finalPath, type)
    }


    private fun printDetailedOutput(path: LinkedList<Int>, type: TerminalType) {
        println("=========== INVENTORY VISUALIZATION ==========")
        println("Type: $type")
        println(" ...  = Empty Slot")
        println("[ 0 ] = 1st Click")
        println("[ 1 ] = 2nd Click")
        println("[ 9 ] = 10th Click")
        println("---------------------------------------------")

        val slotToOrderMap = path.withIndex().associate { it.value to it.index }

        for (row in 0 until Y) {
            val rowStr = StringBuilder()
            for (col in 0 until X) {
                val slotId = row * X + col

                if (! slotToOrderMap.containsKey(slotId)) rowStr.append(" ... ")
                else {
                    val orderNum = slotToOrderMap[slotId] !!
                    val numStr = orderNum.toString()
                    rowStr.append(" [$numStr] ")
                }
            }
            println(rowStr.toString())
        }
        println("---------------------------------------------")
    }

    private fun getRandomList(): LinkedList<Int> {
        val list = LinkedList<Int>()
        var remainingCount = Random.nextInt(5, size - size / 2)
        while (remainingCount > 0) {
            val slot = Random.nextInt(size)
            if (! list.contains(slot)) {
                list.add(slot)
                remainingCount --
            }
        }
        return list
    }

    enum class TerminalType {
        COLORS, REDGREEN, STARTWITH;
    }

    object HumanClickOrder {
        private val sqrt1_2 = sqrt(0.5)

        private var MIDDLE_SLOT = size / 2
        private var lastClickedSlot = MIDDLE_SLOT

        fun getX(type: TerminalType) = when (type) {
            // TerminalType.RUBIX -> 3
            TerminalType.COLORS -> 7
            TerminalType.STARTWITH -> 7
            TerminalType.REDGREEN -> 5
            else -> throw IllegalStateException("Melody/Numbers should not be randomaized")
        }

        fun getY(type: TerminalType) = when (type) {
            //    TerminalType.RUBIX -> 3
            TerminalType.COLORS -> 4
            TerminalType.STARTWITH -> 3
            TerminalType.REDGREEN -> 3
            else -> throw IllegalStateException("Melody/Numbers should not be randomaized")
        }

        fun getBestClick(availableClicks: List<TerminalClick>, type: TerminalType): TerminalClick {
            if (availableClicks.isEmpty()) throw IllegalStateException("Solution list is empty")

            val bestClick = availableClicks.shuffled().minByOrNull { click ->
                getDistance(click.slotId, lastClickedSlot)
            } ?: availableClicks.random()

            lastClickedSlot = bestClick.slotId

            return bestClick
        }

        private fun getPosition(id: Int): Pair<Int, Int> {
            return Pair(id % X, floor((id / Y).toDouble()).toInt())
        }

        private fun getDistance(id1: Int, id2: Int): Double {
            val pos1 = getPosition(id1)
            val pos2 = getPosition(id2)

            val dx = abs(pos1.first - pos2.first).toDouble()
            val dy = abs(pos1.second - pos2.second).toDouble()

            return dx.pow(2) + dy.pow(2) * sqrt1_2
        }
    }
}


