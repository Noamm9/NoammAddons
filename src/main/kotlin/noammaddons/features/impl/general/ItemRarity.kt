package noammaddons.features.impl.general

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DrawSlotEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ItemUtils.ItemRarity.*
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.getRarity
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.ItemUtils.rarityCache
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.RenderUtils.drawSlotOverlay

object ItemRarity: Feature("Draws the rarity of item behind the slot") {
    @JvmField
    val drawOnHotbar = ToggleSetting("Draw on Hotbar", true)
    private val rarityOpacity = SliderSetting("Rarity Opacity", 0, 100, 1, 30)
    override fun init() = addSettings(drawOnHotbar, rarityOpacity)

    @JvmStatic
    fun onSlotDraw(stack: ItemStack?, x: Int, y: Int) {
        if (rarityOpacity.value == 0) return
        if (! inSkyblock) return
        if (stack == null) return
        if (stack.tagCompound !in rarityCache.keys) {
            if (stack.SkyblockID == "") return
            if (stack.lore.any { line -> line == "§f§lClass Passives" }) return
        }

        val rarity = getRarity(stack)
        if (rarity == NONE) return
        val color = rarity.color.withAlpha((rarityOpacity.value))

        GlStateManager.disableDepth()
        drawSlotOverlay(color, x, y, x + 16, y + 16)
        GlStateManager.enableDepth()
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onDrawSlotEvent(event: DrawSlotEvent) = onSlotDraw(
        event.slot.stack,
        event.slot.xDisplayPosition,
        event.slot.yDisplayPosition
    )
}
