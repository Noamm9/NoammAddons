package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.currentClickColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.nextColor
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.MathUtils.center
import com.github.noamm9.utils.MathUtils.toPos
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.phys.Vec3

object WaterBoardSolver {
    private val waterSolutions by lazy {
        DataDownloader.loadJson<Map<String, Map<String, Map<String, List<Double>>>>>("waterSolutions.json")
    }

    private var solution = HashMap<LEVER, List<Double>>()
    private var patternId = - 1
    private var waterLeverTick: Long = - 1

    private var center: BlockPos? = null
    private var rotation: Int? = null


    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Water Board") return
        if (patternId != - 1) return

        center = event.room.centerPos
        rotation = 360 - (event.room.rotation ?: return)

        ThreadUtils.scheduledTaskServer(30, ::solve)
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (patternId == - 1 || solution.isEmpty()) return

        val clicks = solution
            .flatMap { (lever, times) -> times.drop(lever.clickCount).map { lever to it } }
            .sortedBy { (lever, time) -> time + if (lever == LEVER.WATER) 0.01 else 0.0 }

        val nextClick = clicks.firstOrNull()?.first ?: return

        Render3D.renderTracer(ctx, nextClick.getPos().center(), currentClickColor.value, 1.5f)

        if (clicks.size > 1) {
            val secondClick = clicks[1].first
            if (nextClick != secondClick) Render3D.renderLine(
                ctx,
                nextClick.getPos().center(),
                secondClick.getPos().center(),
                nextColor.value, 1.5f
            )
        }

        solution.forEach { (mech, times) ->
            times.drop(mech.clickCount).forEachIndexed { index, timeSeconds ->
                val timeInTicks = (timeSeconds * 20).toInt()

                val displayText = when (waterLeverTick) {
                    - 1L if timeInTicks == 0 -> "§a§lCLICK"
                    - 1L -> {
                        val color = if (timeSeconds < 2) "§c" else if (timeSeconds < 6) "§e" else "§a"
                        "$color${timeSeconds}s"
                    }

                    else -> {
                        val remainingTicks = waterLeverTick + timeInTicks - DungeonListener.currentTime
                        if (remainingTicks > 0) {
                            val remainingSec = remainingTicks / 20.0
                            val color = if (remainingSec < 2) "§c" else if (remainingSec < 6) "§e" else "§a"
                            "$color${remainingSec.toFixed(1)}s"
                        }
                        else "§a§lCLICK"
                    }
                }

                val renderPos = mech.getPos().add(0.5, (index + mech.clickCount) * 0.5 + 1.5, 0.5)
                Render3D.renderString(displayText, renderPos, scale = 1.35f, phase = true)
            }
        }
    }

    fun onInteract(event: PlayerInteractEvent.RIGHT_CLICK.BLOCK) {
        if (solution.isEmpty()) return
        val center = center ?: return
        val rotation = rotation ?: return
        val block = WorldUtils.getBlockAt(event.pos)

        LEVER.entries.find { it.getPos().toPos() == event.pos }?.let { mech ->
            if (mech == LEVER.WATER && waterLeverTick == - 1L) {
                waterLeverTick = DungeonListener.currentTime
            }
            mech.clickCount ++
        }

        if (block is ChestBlock) {
            if (Gate.entries.any { it.isGateClosed(center, rotation) }) return
            reset()
        }
    }

    private fun solve() {
        val center = center ?: return
        val rot = rotation ?: return

        val gates = Gate.entries.joinToString("") {
            if (it.isGateClosed(center, rot)) it.ordinal.toString() else ""
        }

        if (gates.length != 3) return

        patternId = when {
            checkBlock(BlockPos(- 1, 77, 12), Blocks.TERRACOTTA) -> 0
            checkBlock(BlockPos(1, 78, 12), Blocks.EMERALD_BLOCK) -> 1
            checkBlock(BlockPos(- 1, 78, 12), Blocks.DIAMOND_BLOCK) -> 2
            checkBlock(BlockPos(- 1, 78, 12), Blocks.QUARTZ_BLOCK) -> 3
            else -> throw Error("§cWater Solver: Unknown Pattern.")
        }

        solution.clear()
        waterSolutions["$patternId"]?.get(gates)?.forEach { (key, times) ->
            LEVER.fromKey(key)?.let {
                solution[it] = times
            }
        }
    }

    fun reset() {
        LEVER.entries.forEach { it.clickCount = 0 }
        patternId = - 1
        solution.clear()
        waterLeverTick = - 1
        center = null
        rotation = null
    }

    private fun checkBlock(rel: BlockPos, expected: Block): Boolean {
        val center = center ?: return false
        val rot = rotation ?: return false
        val realPos = ScanUtils.getRealCoord(rel, center, rot)
        return WorldUtils.getBlockAt(realPos) == expected
    }

    private enum class Gate(val offset: BlockPos) {
        PURPLE(BlockPos(0, 56, 4)),
        ORANGE(BlockPos(0, 56, 3)),
        BLUE(BlockPos(0, 56, 2)),
        GREEN(BlockPos(0, 56, 1)),
        RED(BlockPos(0, 56, 0));

        fun isGateClosed(center: BlockPos, rotation: Int): Boolean {
            val pos = ScanUtils.getRealCoord(offset, center, rotation)
            val block = WorldUtils.getBlockAt(pos)
            return block.defaultBlockState().`is`(BlockTags.WOOL)
        }
    }

    private enum class LEVER(val offset: Vec3, var clickCount: Int = 0) {
        QUARTZ(Vec3(5.0, 61.0, 5.0)),
        GOLD(Vec3(5.0, 61.0, 0.0)),
        COAL(Vec3(5.0, 61.0, - 5.0)),
        DIAMOND(Vec3(- 5.0, 61.0, 5.0)),
        EMERALD(Vec3(- 5.0, 61.0, 0.0)),
        CLAY(Vec3(- 5.0, 61.0, - 5.0)),
        WATER(Vec3(0.0, 60.0, - 10.0));

        fun getPos(): Vec3 {
            val center = center ?: return Vec3.ZERO
            val rot = rotation ?: return Vec3.ZERO

            val relBlock = BlockPos(offset.x.toInt(), offset.y.toInt(), offset.z.toInt())
            val realBlock = ScanUtils.getRealCoord(relBlock, center, rot)
            return realBlock.toVec()
        }

        companion object {
            fun fromKey(str: String) = when (str) {
                "diamond_block" -> DIAMOND
                "emerald_block" -> EMERALD
                "hardened_clay" -> CLAY
                "quartz_block" -> QUARTZ
                "gold_block" -> GOLD
                "coal_block" -> COAL
                "water" -> WATER
                else -> null
            }
        }
    }
}