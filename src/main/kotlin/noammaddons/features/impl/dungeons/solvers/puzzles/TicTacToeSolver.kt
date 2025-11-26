package noammaddons.features.impl.dungeons.solvers.puzzles

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.init.Blocks
import net.minecraft.item.ItemMap
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraft.util.*
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.events.*
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.RenderUtils.renderManager
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.formatPbPuzzleMessage
import org.lwjgl.opengl.GL11
import kotlin.experimental.and
import kotlin.math.*


object TicTacToeSolver {
    private var inTicTacToe = false
    private var trueStartTime: Long? = null
    private var bestMove: AxisAlignedBB? = null
    private var roomCenter: BlockPos? = null

    @SubscribeEvent
    fun onEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! PuzzleSolvers.ticTacToe.value) return
        if (event.room.data.name != "Tic Tac Toe") return

        inTicTacToe = true
        roomCenter = ScanUtils.getRoomCenter(event.room)
        trueStartTime = System.currentTimeMillis()

        ThreadUtils.scheduledTask(2) {
            bestMove = scanBoard(roomCenter !!)
        }
    }

    @SubscribeEvent
    fun onExit(event: DungeonEvent.RoomEvent.onExit) {
        if (! inTicTacToe) return
        inTicTacToe = false
        trueStartTime = null
        bestMove = null
        roomCenter = null
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        if (! inTicTacToe) return
        inTicTacToe = false
        trueStartTime = null
        bestMove = null
        roomCenter = null
    }

    @SubscribeEvent
    fun onPacket(event: PostPacketEvent.Received) {
        if (! inTicTacToe) return
        val packet = event.packet as? S0EPacketSpawnObject ?: return
        if (packet.type != 71) return // EntityItemFrame

        ThreadUtils.scheduledTask(2) {
            bestMove = scanBoard(roomCenter !!)
        }
    }

    fun scanBoard(center: BlockPos): AxisAlignedBB? {
        val aabb = AxisAlignedBB(
            center.x - 9.0, 65.0, center.z - 9.0,
            center.x + 9.0, 73.0, center.z + 9.0
        )

        val itemFrames = mc.theWorld.getEntitiesWithinAABB(EntityItemFrame::class.java, aabb)
        val itemFramesWithMaps = itemFrames.filter { frame ->
            frame.displayedItem?.let { (it.item as? ItemMap)?.getMapData(it, mc.theWorld) } != null
        }

        if (itemFramesWithMaps.size == 8 && trueStartTime != null) {
            val personalBestsData = personalBests.getData().pazzles
            val previousBest = personalBestsData["Tic Tac Toe"]
            val completionTime = (System.currentTimeMillis() - trueStartTime !!).toDouble()
            val message = formatPbPuzzleMessage("Tic Tac Toe", completionTime, previousBest)

            sendPartyMessage(message)
            clickableChat(message, "/na copy ${message.removeFormatting()}")

            inTicTacToe = false
            trueStartTime = null
            bestMove = null
            roomCenter = null
            return null
        }

        if (itemFramesWithMaps.size == 8 || itemFramesWithMaps.size % 2 == 0) return null

        val board = Array(3) { CharArray(3) }
        var leftmostRow: BlockPos? = null
        var facing = 'X'
        var sign = 1

        for (itemFrame in itemFramesWithMaps) {
            val mapData = (itemFrame.displayedItem.item as ItemMap).getMapData(itemFrame.displayedItem, mc.theWorld)

            sign = if (itemFrame.facingDirection.equalsOneOf(EnumFacing.SOUTH, EnumFacing.WEST)) - 1 else 1
            val itemFramePos = BlockPos(itemFrame.posX, floor(itemFrame.posY), itemFrame.posZ)
            var row = 0

            for (i in 2 downTo 0) {
                val realI = i * sign
                var blockPos = itemFramePos
                if (itemFrame.posX % 0.5 == 0.0) blockPos = itemFramePos.add(realI, 0, 0)
                else if (itemFrame.posZ % 0.5 == 0.0) {
                    blockPos = itemFramePos.add(0, 0, realI)
                    facing = 'Z'
                }

                if (getBlockAt(blockPos).equalsOneOf(Blocks.stone_button, Blocks.air)) {
                    leftmostRow = blockPos
                    row = i
                    break
                }
            }

            val column = (72 - itemFrame.posY.toInt()).takeIf { it in 0 .. 2 } ?: continue
            val byteColor = (mapData.colors[8256] and 255.toByte()).toInt()
            if (byteColor == 114) board[column][row] = 'X'
            else if (byteColor == 33) board[column][row] = 'O'
        }

        val bestMove = TicTacToeUtils.getBestMove(board) - 1

        if (leftmostRow != null) {
            val drawX = (if (facing == 'X') leftmostRow.x - sign * (bestMove % 3) else leftmostRow.x).toDouble()
            val drawY = 72 - floor((bestMove / 3).toDouble())
            val drawZ = (if (facing == 'Z') leftmostRow.z - sign * (bestMove % 3) else leftmostRow.z).toDouble()
            return AxisAlignedBB(drawX, drawY, drawZ, drawX + 1, drawY + 1, drawZ + 1)
        }

        return null
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        bestMove?.offset(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)?.let {
            GlStateManager.pushMatrix()
            RenderUtils.preDraw()
            RenderUtils.disableDepth()
            GL11.glLineWidth(2f)
            RenderUtils.drawOutlinedAABB(it, PuzzleSolvers.ticTacToeColor.value.withAlpha(255))
            RenderUtils.drawFilledAABB(it, PuzzleSolvers.ticTacToeColor.value)
            GL11.glLineWidth(2f)
            RenderUtils.enableDepth()
            RenderUtils.postDraw()
            GlStateManager.popMatrix()
        }
    }

    private object TicTacToeUtils {
        fun getBestMove(board: Array<CharArray>): Int {
            val moves: HashMap<Int, Int> = HashMap()
            for (row in board.indices) {
                for (col in board[row].indices) {
                    if (board[row][col] != '\u0000') continue
                    board[row][col] = 'O'
                    val score = minimax(board, false, 0)
                    board[row][col] = '\u0000'
                    moves[row * 3 + col + 1] = score
                }
            }
            return moves.entries.maxBy { it.value }.key
        }

        fun hasMovesLeft(board: Array<CharArray>): Boolean {
            for (rows in board) {
                for (col in rows) {
                    if (col == '\u0000') return true
                }
            }
            return false
        }

        fun getBoardRanking(board: Array<CharArray>): Int {
            for (row in 0 .. 2) {
                if (board[row][0] == board[row][1] && board[row][0] == board[row][2]) {
                    if (board[row][0] == 'X') return - 10
                    else if (board[row][0] == 'O') return 10
                }
            }

            for (col in 0 .. 2) {
                if (board[0][col] == board[1][col] && board[0][col] == board[2][col]) {
                    if (board[0][col] == 'X') return - 10
                    else if (board[0][col] == 'O') return 10
                }
            }

            if (board[0][0] == board[1][1] && board[0][0] == board[2][2]) {
                if (board[0][0] == 'X') return - 10
                else if (board[0][0] == 'O') return 10
            }
            else if (board[0][2] == board[1][1] && board[0][2] == board[2][0]) {
                if (board[0][2] == 'X') return - 10
                else if (board[0][2] == 'O') return 10
            }

            return 0
        }

        fun minimax(board: Array<CharArray>, max: Boolean, depth: Int): Int {
            val score = getBoardRanking(board)
            if (score == 10 || score == - 10) return score
            if (! hasMovesLeft(board)) return 0

            if (max) {
                var bestScore = - 1000
                for (row in 0 .. 2) {
                    for (col in 0 .. 2) {
                        if (board[row][col] == '\u0000') {
                            board[row][col] = 'O'
                            bestScore = max(bestScore.toDouble(), minimax(board, false, depth + 1).toDouble()).toInt()
                            board[row][col] = '\u0000'
                        }
                    }
                }
                return bestScore - depth
            }
            else {
                var bestScore = 1000
                for (row in 0 .. 2) {
                    for (col in 0 .. 2) {
                        if (board[row][col] == '\u0000') {
                            board[row][col] = 'X'
                            bestScore = min(bestScore.toDouble(), minimax(board, true, depth + 1).toDouble()).toInt()
                            board[row][col] = '\u0000'
                        }
                    }
                }
                return bestScore + depth
            }
        }
    }
}