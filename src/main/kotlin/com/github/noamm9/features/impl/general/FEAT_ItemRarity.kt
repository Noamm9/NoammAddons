package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack


object FEAT_ItemRarity: Feature(name = "Item Rarity", description = "Draws the rarity of item behind the slot") {
    val drawOnHotbar by ToggleSetting("Draw on Hotbar", true)
    private val rarityOpacity by SliderSetting("Rarity Opacity", 30f, 10f, 100f, 1f)
    private val style by DropdownSetting("Rarity Style", 0, listOf("Filled", "Outline", "Filled Outline"))

    override fun init() {
        register<ContainerEvent.Render.Slot.Pre> {
            onSlotDraw(event.context, event.slot.item, event.slot.x, event.slot.y)
        }
    }

    @JvmStatic
    fun onSlotDraw(ctx: GuiGraphics, stack: ItemStack?, x: Int, y: Int) {
        if (! LocationUtils.inSkyblock) return
        if (stack == null) return

        val rarity = ItemUtils.getRarity(stack)
        if (rarity == ItemRarity.NONE) return
        val color = rarity.color.withAlpha(rarityOpacity.value / 100)

        when (style.value) {
            0 -> ctx.fill(x, y, x + 16, y + 16, color.rgb)
            1 -> Render2D.drawBorder(ctx, x, y, 16, 16, color)
            2 -> {
                ctx.fill(x, y, x + 16, y + 16, color.rgb)
                Render2D.drawBorder(ctx, x, y, 16, 16, rarity.color)
            }
        }
    }
}