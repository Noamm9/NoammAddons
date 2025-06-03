package noammaddons.features.impl.general

import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.enchantNameToID
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.NumbersUtils.format

object ItemsPrice: Feature("Displays the price of items in the tooltip") {
    val showNpcPrice = ToggleSetting("NPC Price", true)
    val showAhPrice = ToggleSetting("AH Price", true)
    val showBzPrice = ToggleSetting("Bazaar Price", true)
    override fun init() = addSettings(showNpcPrice, showAhPrice, showBzPrice)

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRender(event: ItemTooltipEvent) {
        val stack = event.itemStack ?: return
        val sbId = stack.SkyblockID ?: return
        val displayStackSize = stack.stackSize.takeIf { it > 1 }
        val id = if (sbId == "ENCHANTED_BOOK") enchantNameToID(stack.lore[0]) else sbId

        val marketPrice = when {
            showAhPrice.value && id in ahData -> ahData[id]
            showBzPrice.value && id in bzData -> bzData[id]?.sellPrice
            else -> null
        }

        marketPrice?.let {
            val tooltip = formatPriceTooltip(it, displayStackSize)
            event.toolTip.add("&b&lPrice: $tooltip".addColor())
        }

        if (showNpcPrice.value) {
            npcData[id]?.let {
                val tooltip = formatPriceTooltip(it, displayStackSize)
                event.toolTip.add("&b&lNPC Price: $tooltip".addColor())
            }
        }
    }

    private fun formatPriceTooltip(price: Double, stackSize: Int? = null): String {
        return stackSize?.takeIf { it > 1 }?.let {
            "&6${format(price * it)} &7($it×)"
        } ?: "&6${format(price)}"
    }
}

