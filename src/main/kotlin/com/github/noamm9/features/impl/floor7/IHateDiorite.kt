package com.github.noamm9.features.impl.floor7

//#if CHEAT

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object IHateDiorite: Feature("I Hate Diorite") {
    private val positions by lazy {
        val blockStates = mapOf(
            "GreenArray" to Blocks.GREEN_STAINED_GLASS.defaultBlockState(),
            "YellowArray" to Blocks.YELLOW_STAINED_GLASS.defaultBlockState(),
            "PurpleArray" to Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
            "RedArray" to Blocks.RED_STAINED_GLASS.defaultBlockState()
        )

        buildList {
            DataDownloader.loadJson<Map<String, List<Map<String, Double>>>>("iHateDioriteBlocks.json").forEach { (key, coords) ->
                coords.forEach { add(BlockPos(it["x"] !!.toInt(), it["y"] !!.toInt(), it["z"] !!.toInt()) to (blockStates[key] ?: return@forEach)) }
            }
        }
    }

    private val cursor = BlockPos.MutableBlockPos()

    override fun init() {
        register<TickEvent.Start> {
            if (LocationUtils.F7Phase != 2) return@register
            for ((basePos, glassState) in positions) for (yOffset in 0 .. 37) {
                cursor.set(basePos.x, basePos.y + yOffset, basePos.z)
                if (WorldUtils.getBlockAt(cursor).equalsOneOf(Blocks.DIORITE, Blocks.POLISHED_DIORITE)) {
                    WorldUtils.setBlockAt(cursor, glassState)
                }
            }
        }
    }
}
//#endif