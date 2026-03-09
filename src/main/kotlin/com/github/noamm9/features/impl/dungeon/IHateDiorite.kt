package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object IHateDiorite: Feature("I Hate Diorite") {
    private val blockStates = mapOf(
        "GreenArray" to Blocks.GREEN_STAINED_GLASS.defaultBlockState(),
        "YellowArray" to Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),
        "PurpleArray" to Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
        "RedArray" to Blocks.RED_STAINED_GLASS.defaultBlockState()
    )

    private val flattenedPositions by lazy {
        val data = DataDownloader.loadJson<Map<String, List<Map<String, Double>>>>("iHateDioriteBlocks.json")
        val list = mutableListOf<Pair<BlockPos, BlockState>>()

        data.forEach { (key, coords) ->
            val state = blockStates[key] ?: return@forEach
            coords.forEach { c ->
                list.add(BlockPos(c["x"] !!.toInt(), c["y"] !!.toInt(), c["z"] !!.toInt()) to state)
            }
        }
        list
    }

    private val cursor = BlockPos.MutableBlockPos()

    override fun init() {
        register<TickEvent.Start> {
            if (LocationUtils.F7Phase != 2) return@register
            val level = mc.level ?: return@register

            for ((basePos, glassState) in flattenedPositions) {
                for (yOffset in 0 .. 37) {
                    cursor.set(basePos.x, basePos.y + yOffset, basePos.z)
                    val currentState = level.getBlockState(cursor)

                    if (currentState.`is`(Blocks.DIORITE) || currentState.`is`(Blocks.POLISHED_DIORITE)) {
                        WorldUtils.setBlockAt(cursor, glassState)
                    }
                }
            }
        }
    }
}