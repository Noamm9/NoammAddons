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
import net.minecraft.world.item.Items
import kotlin.jvm.optionals.getOrDefault

object ItemOverlays: Feature("Display info about various items.") {
    private val cakeNumbers by ToggleSetting("Cake Numbers").withDescription("Displays the year of the cake in the New Year Cake Bag.")
    private val enchantedBookAbbreviation by ToggleSetting("Enchanted Book Abbreviation").withDescription("Shows the abbreviated name of books with only 1 enchantment")
    private val enchantedBookLevel by ToggleSetting("Enchanted Book Level").withDescription("Shows the tier of books with only 1 enchantment.")

    override fun init() {
        register<ContainerEvent.Render.Slot.Post> {
            if (! cakeNumbers.value) return@register
            if (! LocationUtils.inSkyblock) return@register
            if (! event.slot.item.`is`(Items.CAKE)) return@register
            val name = event.slot.item.hoverName.unformattedText
            if ("New Year Cake (Year " !in name) return@register
            val year = name.remove("New Year Cake (Year ", ")").trim()
            event.context.drawCenteredString("&b$year", event.slot.x + 8, event.slot.y + 8, scale = 0.8)
        }

        register<ContainerEvent.Render.Slot.Post> {
            if (! enchantedBookAbbreviation.value && ! enchantedBookLevel.value) return@register
            if (! LocationUtils.inSkyblock) return@register
            if (! event.slot.item.`is`(Items.ENCHANTED_BOOK)) return@register
            if (! event.slot.item.skyblockId.startsWith("ENCHANTMENT_")) return@register
            event.slot.item.customData.getCompoundOrEmpty("enchantments").takeIf { it.keySet().size == 1 }?.let { enchantments ->
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

                if (enchantedBookAbbreviation.value) event.context.drawString(prefix, event.slot.x, event.slot.y, scale = scale)
                if (enchantedBookLevel.value && level.isNotEmpty()) event.context.drawString(level, event.slot.x + 17 - level.width(), event.slot.y + 9)
            }
        }
    }
}