package noammaddons.features.general

import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.enchantNameToID
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.NumbersUtils.format

object ShowItemsPrice: Feature() {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRender(event: ItemTooltipEvent) {
        if (! config.showItemPrice) return
        val stack = event.itemStack ?: return
        val sbId = stack.SkyblockID ?: return
        val npcPrice = npcData[sbId]

        val id = if (sbId == "ENCHANTED_BOOK") enchantNameToID(stack.lore[0]) else sbId
        val Price = ahData[id] ?: bzData[id]?.sellPrice

        Price?.let {
            event.toolTip.add(
                when (stack.stackSize) {
                    1 -> "&b&lAH Price: &6${format(it)}"
                    else -> "&b&lPrice: &6${format(it * stack.stackSize)} &7(${stack.stackSize}x)"
                }.addColor()
            )
        }

        npcPrice?.let {
            event.toolTip.add(
                when (stack.stackSize) {
                    1 -> "&b&lNPC Price: &6${format(it)}"
                    else -> "&b&lNPC Price: &6${format(it * stack.stackSize)} &7(${stack.stackSize}x)"
                }.addColor()
            )
        }
    }
}

