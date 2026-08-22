package com.github.noamm9.features.impl.general

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay
import com.github.noamm9.init.NetworkLoop
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.utils.NumbersUtils.formatComma
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.marketId
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils.inSkyblock
import gg.essential.universal.UKeyboard
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

object ItemTooltip: Feature("Adds item information and controls to item tooltips") {
    private val showPrices by ToggleSetting("Item Prices").section("Item Information").withDescription("Shows Bazaar and Lowest BIN prices")
    private val showNpcSellPrice by ToggleSetting("NPC Sell Price").withDescription("Shows the price an item sells for to NPCs").showIf { showPrices.value }
    private val showItemQuality by ToggleSetting("Item Quality").withDescription("Shows the base stats boost of dungeon items and their floor")

    private val scrollableTooltips by ToggleSetting("Scrollable Tooltips").section("Scrollable Tooltips").withDescription("Allows item tooltips to be moved and scaled with the scroll wheel")
    @JvmStatic val tooltipScale by SliderSetting("Tooltip Scale", 100, 30, 150, 0.1).withDescription("The size of the tooltip").showIf { scrollableTooltips.value }
    private val scrollSpeed by SliderSetting("Scroll Speed", 3, 1, 10, 1).withDescription("How fast the tooltip scrolls").showIf { scrollableTooltips.value }
    private val scaleSpeed by SliderSetting("Scale Speed", 3, 1, 10, 1).withDescription("How fast the tooltip scales").showIf { scrollableTooltips.value }

    @JvmField var scrollAmountX = 0f
    @JvmField var scrollAmountY = 0f
    @JvmField var scaleOverride = 0f

    @JvmStatic
    var slot = 0
        set(value) {
            if (value == field) return
            resetScroll()
            field = value
        }

    override fun init() {
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            if (! enabled || ! scrollableTooltips.value) return@register
            val containerScreen = screen as? AbstractContainerScreen<*> ?: return@register
            ScreenMouseEvents.afterMouseScroll(containerScreen).register { _, _, _, _, verticalAmount, _ ->
                if (containerScreen is ContainerScreen && StorageOverlay.activeFor(containerScreen) != null) return@register false
                val hoveredSlot = (containerScreen as IAbstractContainerScreen).hoveredSlot ?: return@register false
                if (hoveredSlot.item.isEmpty) return@register false

                applyScroll(verticalAmount)
                true
            }
        }

        register<ContainerEvent.Render.Tooltip> {
            if (! inSkyblock) return@register

            addItemQuality(event)

            if (! showPrices.value) return@register

            val quantity = event.stack.count
            val itemId = event.stack.marketId

            NetworkLoop.getBazaarPrice(itemId)?.let { price ->
                addPriceLine(event.lore, "Bazaar Buy", price.buy, quantity)
                addPriceLine(event.lore, "Bazaar Sell", price.sell, quantity)
            } ?: NetworkLoop.getLowestBin(itemId)?.let { price ->
                addPriceLine(event.lore, "Lowest BIN", price, quantity)
            }

            if (showNpcSellPrice.value) NetworkLoop.getNpcSellPrice(event.stack.skyblockId)?.let { price ->
                if (price > 0L) event.lore.add(Component.literal("§eNPC Sell: §6${formatComma(price)}"))
            }
        }
    }

    private fun addPriceLine(lore: MutableList<Component>, label: String, unitPrice: Long, quantity: Int) {
        if (unitPrice <= 0L) return
        val showStackPrice = quantity > 1 && isShiftDown()
        val price = if (showStackPrice) unitPrice * quantity else unitPrice
        val stackInfo = if (showStackPrice) " §8(${formatComma(quantity)}x ${formatComma(unitPrice)})" else ""
        lore.add(Component.literal("§e$label: §6${formatComma(price)}$stackInfo"))
    }

    private fun addItemQuality(event: ContainerEvent.Render.Tooltip) {
        if (! showItemQuality.value) return
        val data = event.stack.customData
        val boost = data.getInt("baseStatBoostPercentage").getOrNull()?.takeIf { it > 0 } ?: return
        val req = data.getString("dungeon_skill_req").getOrDefault("")
        val tier = data.getInt("item_tier").getOrDefault(0)

        val floor = when {
            req.isEmpty() && tier > 0 -> "§aE"
            req.isEmpty() -> "§bF$tier"
            else -> {
                val (dungeon, level) = req.split(':', limit = 2)
                val levelReq = level.toIntOrNull() ?: 0
                if (dungeon == "CATACOMBS") {
                    if (levelReq - tier > 19) "§4M${tier - 3}" else "§aF$tier"
                }
                else "§b$dungeon $tier"
            }
        }

        val color = when {
            boost <= 17 -> "§c"
            boost <= 33 -> "§e"
            boost <= 49 -> "§a"
            else -> "§b"
        }
        event.lore.add(Component.literal("§6Quality Bonus: $color+$boost% §7($floor§7)"))
    }

    @JvmStatic
    fun resetScroll() {
        scrollAmountX = 0f
        scrollAmountY = 0f
        scaleOverride = 0f
    }

    internal fun applyScroll(verticalAmount: Double) {
        if (! scrollableTooltips.value) return
        val scroll = (verticalAmount * scrollSpeed.value).toFloat()
        val holdingShift = isShiftDown()
        val holdingCtrl = UKeyboard.isCtrlKeyDown()

        when {
            holdingShift && ! holdingCtrl -> scrollAmountX -= scroll
            ! holdingShift && holdingCtrl -> {
                val baseScale = tooltipScale.value.toFloat() / 100f
                val nextScale = (baseScale + scaleOverride / 10f + (verticalAmount / 100f).toFloat() * scaleSpeed.value.toFloat()).coerceIn(0.3f, 2.0f)
                scaleOverride = (nextScale - baseScale) * 10f
            }

            else -> scrollAmountY += scroll
        }
    }

    private fun isShiftDown(): Boolean = UKeyboard.isShiftKeyDown()

    @JvmStatic fun isScrollingEnabled() = enabled && scrollableTooltips.value
}