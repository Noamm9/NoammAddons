package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.withDescription
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

object SecretHitboxes: Feature("Changes the hitboxes of secret blocks to be larger.") {

    @JvmStatic
    val lever by ToggleSetting("Lever").withDescription("Full block Lever hitbox.")

    private val BlackListedLevers =
        listOf(
            BlockPos(61, 136, 142), BlockPos(60, 136, 142), BlockPos(59, 136, 142),
            BlockPos(62, 135, 142), BlockPos(61, 135, 142), BlockPos(59, 135, 142), BlockPos(58, 135, 142),
            BlockPos(62, 134, 142), BlockPos(61, 134, 142), BlockPos(59, 134, 142), BlockPos(58, 134, 142),
            BlockPos(61, 133, 142), BlockPos(60, 133, 142), BlockPos(59, 133, 142)
        )
    @JvmStatic
    fun isValidLever(pos: BlockPos): Boolean {
        for (blockPos in BlackListedLevers) {
            if (blockPos == pos) return false
        }
        return true
    }

    @JvmStatic
    val button by ToggleSetting("Button").withDescription("Full block button hitbox.")

    @JvmStatic
    val skull by ToggleSetting("Skulls").withDescription("Full block Skull hitbox.")

    @JvmStatic
    val mushroom by ToggleSetting("Mushroom").withDescription("Full block Mushroom hitbox.")

    @JvmStatic
    fun getButtonShape(state: BlockState): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val direction = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
        val powered = state.getValue(ButtonBlock.POWERED)

        val f2 = (if (powered) 1 else 2) / 16.0
        return when (face) {
            AttachFace.CEILING -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            AttachFace.FLOOR -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
            else -> when (direction) {
                Direction.EAST -> Shapes.box(0.0, 0.0, 0.0, f2, 1.0, 1.0)
                Direction.WEST -> Shapes.box(1.0 - f2, 0.0, 0.0, 1.0, 1.0, 1.0)
                Direction.SOUTH -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, f2)
                Direction.NORTH -> Shapes.box(0.0, 0.0, 1.0 - f2, 1.0, 1.0, 1.0)
                Direction.UP -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
                Direction.DOWN -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            }
        }
    }
}