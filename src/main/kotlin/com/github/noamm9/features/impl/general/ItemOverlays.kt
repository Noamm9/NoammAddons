package com.github.noamm9.features.impl.general

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.uppercaseFirst
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.jvm.optionals.getOrDefault

object ItemOverlays: Feature("Display info about various items.") {
    private val cakeNumbers by ToggleSetting("Cake Numbers").withDescription("Displays the year of the cake in the New Year Cake Bag.")
    private val enchantedBookAbbreviation by ToggleSetting("Enchanted Book Abbreviation").withDescription("Shows the abbreviated name of books with only 1 enchantment")
    private val enchantedBookLevel by ToggleSetting("Enchanted Book Level").withDescription("Shows the tier of books with only 1 enchantment.")

    override fun init() {
        register<ContainerEvent.Render.Slot.Post> {
            onSlotDraw(event.context, event.slot.item, event.slot.x, event.slot.y)
        }
    }

    fun onSlotDraw(context: GuiGraphicsExtractor, stack: ItemStack, x: Int, y: Int) {
        if (! enabled || ! LocationUtils.inSkyblock) return
        if (cakeNumbers.value && stack.`is`(Items.CAKE)) {
            val name = stack.hoverName.unformattedText
            if ("New Year Cake (Year " in name) {
                val year = name.remove("New Year Cake (Year ", ")").trim()
                context.drawCenteredString("&b$year", x + 8, y + 8, scale = 0.8)
            }
        }

        if ((enchantedBookAbbreviation.value || enchantedBookLevel.value) && stack.`is`(Items.ENCHANTED_BOOK) && stack.skyblockId.startsWith("ENCHANTMENT_")) {
            stack.customData.getCompoundOrEmpty("enchantments").takeIf { it.keySet().size == 1 }?.let { enchantments ->
                val name = enchantments.keySet().first()
                val level = enchantments.getInt(name).getOrDefault("").toString()
                var scale = 0.8

                val prefix = run {
                    val parts = name.split("_")
                    if (parts[0] == "ultimate") "§d§l" + parts.drop(1).joinToString("") { s -> s[0].uppercase() }.also {
                        if (parts.size > 3) scale = 0.6
                    }
                    else if (parts.size > 1) parts.joinToString("") { s -> s[0].uppercase() }
                    else parts[0].take(3).uppercaseFirst() + "."
                }

                if (enchantedBookAbbreviation.value) context.drawString(prefix, x, y, scale = scale)
                if (enchantedBookLevel.value && level.isNotEmpty()) context.drawString(level, x + 17 - level.width(), y + 9)
            }
        }
    }
}