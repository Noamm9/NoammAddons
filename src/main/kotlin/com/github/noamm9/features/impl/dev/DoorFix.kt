package com.github.noamm9.features.impl.dev

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoorHingeSide
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

object DoorFix: Feature("Fixes the door rotations for s3", toggled = true) {
    override fun toggle() = Unit
    override fun init() {
        register<TickEvent.End> {
            if (LocationUtils.P3Section == 3) doorPositions.forEach {
                if (WorldUtils.getBlockAt(it) != Blocks.AIR) WorldUtils.setBlockAt(it, doorState)
            }
        }
    }

    val doorPositions = buildList {
        for (i in 0 .. 6) {
            if (i != 1) add(BlockPos(1, 112 + i * 4, 104))
            if (i < 6) add(BlockPos(1, 113 + i * 4, 86))
            add(BlockPos(1, 112 + i * 4, 68))
        }
    }

    val doorState = Blocks.IRON_DOOR.defaultBlockState()
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
        .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT)
        .setValue(BlockStateProperties.OPEN, false)
}