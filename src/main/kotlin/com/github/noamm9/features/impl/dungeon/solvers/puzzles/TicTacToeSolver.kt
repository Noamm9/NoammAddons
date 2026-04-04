package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.color
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.prediction
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.predictionColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.preventMissClick
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.NoammRenderLayers
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object TicTacToeSolver {
    private var inTicTacToe = false
    private var roomCenter: BlockPos? = null
    private var rotation: Int? = null

    private var bestMoves = CopyOnWriteArrayList<BlockPos>()
    private var aiPredictions = CopyOnWriteArrayList<BlockPos>()
    private var prefirePredictions = CopyOnWriteArrayList<BlockPos>()

    private const val UNPLAYED = '\u0000'

    fun onStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (event.room.name != "Tic Tac Toe") return
        if (event.newState != RoomState.DISCOVERED) return
        solveAsync()
    }

    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Tic Tac Toe") return
        inTicTacToe = true
        roomCenter = event.room.centerPos
        rotation = event.room.rotation
        solveAsync()
    }

    fun onPacket(event: MainThreadPacketReceivedEvent.Pre) {
        if (! inTicTacToe) return
        when (val packet = event.packet) {
            is ClientboundAddEntityPacket -> {
                if (packet.type != EntityType.ITEM_FRAME) return
                solveAsync()
            }

            is ClientboundMapItemDataPacket -> solveAsync()
        }
    }


    fun onInteract(event: PlayerInteractEvent.RIGHT_CLICK.BLOCK) {
        if (! inTicTacToe || ! preventMissClick.value) return
        if (LocationUtils.inBoss) return
        if (WorldUtils.getBlockAt(event.pos) != Blocks.STONE_BUTTON) return
        if (event.pos !in bestMoves) event.isCanceled = true

    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! inTicTacToe) return

        bestMoves.forEach { renderTTTBox(ctx, it, color.value) }

        if (prediction.value) {
            aiPredictions.forEach { renderTTTBox(ctx, it, Color.RED) }
            prefirePredictions.forEach { renderTTTBox(ctx, it, predictionColor.value) }
        }
    }


    private fun solveAsync() {
        ThreadUtils.scheduledTaskServer(3) {
            ThreadUtils.async {
                val center = roomCenter ?: return@async
                scanBoard(center)
            }
        }
    }

    fun reset() {
        inTicTacToe = false
        bestMoves.clear()
        prefirePredictions.clear()
        aiPredictions.clear()
        roomCenter = null
        rotation = null
    }

    private fun scanBoard(center: BlockPos) {
        val level = mc.level ?: return
        val aabb = AABB(center.x - 9.0, 65.0, center.z - 9.0, center.x + 9.0, 73.0, center.z + 9.0)

        val frames = level.getEntitiesOfClass(ItemFrame::class.java, aabb).filter {
            it.item.item is MapItem && it.item.has(DataComponents.MAP_ID)
        }

        if (frames.size == 8) return reset()
        if (frames.size % 2 == 0) return

        val board = CharArray(9) { UNPLAYED }
        var leftmostRow: BlockPos? = null
        var facing = 'X'
        var sign = 1

        for (itemFrame in frames) {
            val mapId = itemFrame.item.get(DataComponents.MAP_ID) ?: continue
            val mapData = level.getMapData(mapId) ?: continue
            val direction = itemFrame.direction
            sign = if (direction.equalsOneOf(Direction.SOUTH, Direction.WEST)) - 1 else 1
            val itemFramePos = itemFrame.blockPosition()

            var row = 0
            for (i in 2 downTo 0) {
                val realI = i * sign
                val blockPos = if (itemFrame.x % 0.5 == 0.0) itemFramePos.offset(realI, 0, 0)
                else {
                    facing = 'Z'; itemFramePos.offset(0, 0, realI)
                }

                if (WorldUtils.getBlockAt(blockPos).equalsOneOf(Blocks.STONE_BUTTON, Blocks.AIR)) {
                    leftmostRow = blockPos
                    row = i
                    break
                }
            }

            val column = (72 - itemFrame.y.toInt()).takeIf { it in 0 .. 2 } ?: continue
            val byteColor = (mapData.colors[8256].toInt() and 0xFF)

            val index = column * 3 + row
            if (byteColor == 114) board[index] = 'X' // AI
            else if (byteColor == 33) board[index] = 'O' // Player
        }

        if (leftmostRow == null) return
        bestMoves.clear(); aiPredictions.clear(); prefirePredictions.clear()

        val playerBestIndices = TicTacToeUtils.findBestMoves(board, 'O', 'X')
        playerBestIndices.forEach { bestMoves.add(indexToPos(it, leftmostRow, facing, sign)) }

        if (prediction.value && playerBestIndices.isNotEmpty()) {
            val myMove = playerBestIndices.first()
            val simBoard = board.copyOf().apply { this[myMove] = 'O' }

            val aiResponses = TicTacToeUtils.findBestMoves(simBoard, 'X', 'O')
            aiResponses.forEach { aiIdx ->
                aiPredictions.add(indexToPos(aiIdx, leftmostRow, facing, sign))

                val prefireBoard = simBoard.copyOf().apply { this[aiIdx] = 'X' }
                if (! TicTacToeUtils.isWon(prefireBoard)) {
                    TicTacToeUtils.findBestMoves(prefireBoard, 'O', 'X').forEach { preIdx ->
                        val pos = indexToPos(preIdx, leftmostRow, facing, sign)
                        if (pos !in prefirePredictions) prefirePredictions.add(pos)
                    }
                }
            }
        }

        if (prefirePredictions.size == 7) prefirePredictions.clear()
    }

    private fun indexToPos(index: Int, leftmostRow: BlockPos, facing: Char, sign: Int): BlockPos {
        val row = index % 3
        val col = index / 3
        val drawX = if (facing == 'X') leftmostRow.x - sign * row else leftmostRow.x
        val drawY = 72 - col
        val drawZ = if (facing == 'Z') leftmostRow.z - sign * row else leftmostRow.z
        return BlockPos(drawX, drawY, drawZ)
    }

    private fun renderTTTBox(ctx: RenderContext, pos: BlockPos, color: Color) {
        val rotation = rotation ?: return
        if (WorldUtils.getBlockAt(pos) != Blocks.STONE_BUTTON) return
        val cam = ctx.camera.position.reverse()

        val halfWidth = 0.2
        val halfHeight = 0.13
        val thickness = 0.2

        val bx = pos.x.toDouble()
        val by = pos.y.toDouble()
        val bz = pos.z.toDouble()
        val minY = by + 0.5 - halfHeight
        val maxY = by + 0.5 + halfHeight

        var minX: Double
        var maxX: Double
        var minZ: Double
        var maxZ: Double

        when (rotation) {
            0 -> {
                minX = bx
                maxX = bx + thickness
                minZ = bz + 0.5 - halfWidth
                maxZ = bz + 0.5 + halfWidth
            }

            90 -> {
                minX = bx + 0.5 - halfWidth
                maxX = bx + 0.5 + halfWidth
                minZ = bz
                maxZ = bz + thickness
            }

            180 -> {
                minX = bx + 1.0 - thickness
                maxX = bx + 1.0
                minZ = bz + 0.5 - halfWidth
                maxZ = bz + 0.5 + halfWidth
            }

            270 -> {
                minX = bx + 0.5 - halfWidth
                maxX = bx + 0.5 + halfWidth
                minZ = bz + 1.0 - thickness
                maxZ = bz + 1.0
            }

            else -> return
        }

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(cam.x, cam.y, cam.z)

        ShapeRenderer.addChainedFilledBoxVertices(
            ctx.matrixStack,
            ctx.consumers.getBuffer(NoammRenderLayers.FILLED_THROUGH_WALLS),
            minX, minY, minZ,
            maxX, maxY, maxZ,
            color.red / 255f, color.green / 255f, color.blue / 255f, 0.7f
        )

        ctx.matrixStack.popPose()
    }

    /**
     * Minimax algorithm for Tic-tac-toe
     * based off https://gnoht.com/til/ttt-with-minimax/
     */
    private object TicTacToeUtils {
        // Player = 'O' (Maximizer), AI = 'X' (Minimizer)

        private val WIN_SETS = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(6, 4, 2)
        )

        fun isWon(p: CharArray): Boolean {
            for (ws in WIN_SETS) {
                if (p[ws[0]] != '\u0000' && p[ws[0]] == p[ws[1]] && p[ws[0]] == p[ws[2]]) return true
            }
            return false
        }

        private fun getAvailableMoves(p: CharArray) = p.indices.filter { p[it] == '\u0000' }

        fun findBestMoves(board: CharArray, player: Char, opponent: Char): List<Int> {
            val moves = getAvailableMoves(board)
            if (moves.isEmpty()) return emptyList()

            val moveScores = mutableMapOf<Int, Double>()
            var bestScore = Double.NEGATIVE_INFINITY

            for (m in moves) {
                val sim = board.copyOf().apply { this[m] = player }
                val score = minimax(sim, player, opponent, false, 1)
                moveScores[m] = score
                if (score > bestScore) bestScore = score
            }

            return moveScores.filter { it.value >= bestScore - 0.0001 }.keys.toList()
        }

        private fun minimax(p: CharArray, ai: Char, opp: Char, maximizing: Boolean, depth: Int): Double {
            val won = isWon(p)
            val moves = getAvailableMoves(p)

            if (won || moves.isEmpty()) {
                if (won) return if (! maximizing) 1.0 / depth else - 1.0 / depth
                return 0.0
            }

            return if (maximizing) {
                var res = Double.NEGATIVE_INFINITY
                for (m in moves) {
                    val sim = p.copyOf().apply { this[m] = ai }
                    res = maxOf(res, minimax(sim, ai, opp, false, depth + 1))
                }
                res
            }
            else {
                var res = Double.POSITIVE_INFINITY
                for (m in moves) {
                    val sim = p.copyOf().apply { this[m] = opp }
                    res = minOf(res, minimax(sim, ai, opp, true, depth + 1))
                }
                res
            }
        }
    }
}

