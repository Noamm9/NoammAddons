package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack

abstract class PlayerInteractEvent(val item: ItemStack?): Event(true) {
    sealed class LEFT_CLICK(item: ItemStack?): PlayerInteractEvent(item) {
        class AIR(item: ItemStack?): LEFT_CLICK(item)
        class BLOCK(item: ItemStack?, val pos: BlockPos): LEFT_CLICK(item)
        class ENTITY(item: ItemStack?, val entity: Entity): LEFT_CLICK(item)
    }

    abstract class RIGHT_CLICK(item: ItemStack?): PlayerInteractEvent(item) {
        class AIR(item: ItemStack?): RIGHT_CLICK(item)
        class BLOCK(item: ItemStack?, val pos: BlockPos): RIGHT_CLICK(item)
        class ENTITY(item: ItemStack?, val entity: Entity): RIGHT_CLICK(item)
    }
}