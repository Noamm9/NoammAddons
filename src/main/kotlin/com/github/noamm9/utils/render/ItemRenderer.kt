package com.github.noamm9.utils.render

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack

object ItemRenderer {
    fun drawBatchedItemStack(ctx: GuiGraphicsExtractor, item: ItemStack, x: Int, y: Int, scale: Float = 1f) {
        if (item.isEmpty) return
        ctx.pose().pushMatrix()
        ctx.pose().translate(x + 8f, y + 8f)
        ctx.pose().scale(scale)
        ctx.item(item, -8, -8)
        ctx.pose().popMatrix()
    }
}