package noammaddons.features.general

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DrawSlotEvent
import noammaddons.features.Feature
import noammaddons.noammaddons
import noammaddons.utils.ItemUtils.ItemRarity.*
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.getRarity
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.ItemUtils.rarityCache
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.RenderUtils.drawSlotOverlay
import kotlin.math.roundToInt

object ShowItemRarity: Feature() {

    @JvmStatic
    fun onSlotDraw(stack: ItemStack?, x: Int, y: Int) {
        if (! config.DrawItemRarity) return
        if (config.ItemRarityOpacity == 0f) return
        if (! inSkyblock) return
        if (stack == null) return
        if (stack.tagCompound !in rarityCache.keys) {
            if (stack.SkyblockID == "") return
            if (stack.lore.any { line -> line == "§f§lClass Passives" }) return
        }

        val rarity = getRarity(stack)
        if (rarity == NONE) return

        ///  modMessage(config.ItemRarityOpacity * 255)
        try {
            val color = rarity.color.withAlpha((config.ItemRarityOpacity * 255).roundToInt())
            val x2 = x + 16
            val y2 = y + 16

            GlStateManager.pushMatrix()
            GlStateManager.disableDepth()

            drawSlotOverlay(color, x, y, x2, y2)

            GlStateManager.enableDepth()
            GlStateManager.popMatrix()
        }
        catch (e: Exception) {
            noammaddons.Logger.error("Failed to draw item rarity", e)
        }

    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onDrawSlot(event: DrawSlotEvent) {
        onSlotDraw(event.slot.stack, event.slot.xDisplayPosition, event.slot.yDisplayPosition)
    }
}
