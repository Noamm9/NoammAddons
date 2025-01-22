package noammaddons.features.general

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.ItemUtils.ItemRarity.NONE
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.getRarity
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.RenderHelper.applyAlpha
import noammaddons.utils.RenderUtils.drawSlotOverlay
import noammaddons.utils.Utils.isNull

object DrawItemRarity {
    /**
     * Draws the rarity of the item in the inventory
     *
     * @see noammaddons.mixins.MixinRenderItem
     */
    @JvmStatic
    fun onSlotDraw(stack: ItemStack, x: Int, y: Int) {
        if (! config.DrawItemRarity) return
        if (config.ItemRarityOpacity == 0f) return
        if (! inSkyblock) return
        if (stack.isNull()) return
        if (stack.SkyblockID.isBlank()) return
        if (stack.lore.any { line -> line == "§f§lClass Passives" }) return

        val rarity = getRarity(stack)
        if (rarity == NONE) return
        val color = rarity.color.applyAlpha(config.ItemRarityOpacity * 255)
        val x2 = x + 16
        val y2 = y + 16

        GlStateManager.pushMatrix()
        GlStateManager.disableDepth()

        drawSlotOverlay(color, x, y, x2, y2)

        GlStateManager.enableDepth()
        GlStateManager.popMatrix()
    }

    /*
    @JvmStatic
    fun onSlotDraw(index: Int, x: Int, y: Int) {
        if (! config.DrawItemRarity) return
        if (config.ItemRarityOpacity == 0f) return
        if (! inSkyblock) return
        val stack = getHotbar()[index]
        if (stack.isNull()) return
        if (stack.SkyblockID.isBlank()) return

        val rarity = getRarity(stack).color
        val opacity = (config.ItemRarityOpacity * 255).toInt()
        val x2 = x + 16
        val y2 = y + 16

        GlStateManager.pushMatrix()
        GlStateManager.disableDepth()

        drawSlotOverlay(rarity.applyAlpha(opacity), x, y, x2, y2)

        GlStateManager.enableDepth()
        GlStateManager.popMatrix()
    }*/
}
