package com.github.noamm9.features.impl.general

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.checkSlot
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawTexture
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * @see com.github.noamm9.mixin.MixinGui
 */
object FEAT_ItemRarity: Feature(name = "Item Rarity", description = "Draws the rarity of item behind the slot.") {
    @JvmStatic val drawOnHotbar by ToggleSetting("Draw on Hotbar", true)
    private val rarityOpacity by SliderSetting("Rarity Opacity", 30f, 10f, 100f, 1f)
    private val style by DropdownSetting("Rarity Style", 0, listOf("Filled", "Outline", "Filled Outline", "Circle"))
    private val colorStyle by DropdownSetting("Color Style", 0, listOf("Default", "Hypixel"))
    private val circleTexture = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/circle.png")

    override fun init() {
        register<ContainerEvent.Render.Slot.Pre> {
            onSlotDraw(event.context, event.slot.item, event.slot.x, event.slot.y)
        }
    }

    @JvmStatic
    fun onSlotDraw(ctx: GuiGraphicsExtractor, stack: ItemStack?, x: Int, y: Int) {
        if (! LocationUtils.inSkyblock) return
        if (stack == null) return

        val rarity = ItemUtils.getRarity(stack)
        if (rarity == ItemRarity.NONE) return
        val rarityColor = if (colorStyle.value == 1) ItemRarity.getHypixelColor(rarity) else rarity.color
        val color = rarityColor.withAlpha(rarityOpacity.value / 100)

        if (checkSlot(x, y, - 1)) when (style.value) {
            0 -> ctx.fill(x, y, x + 16, y + 16, color.rgb)
            1 -> ctx.drawBorder(x, y, 16, 16, color)
            2 -> {
                ctx.fill(x, y, x + 16, y + 16, color.rgb)
                ctx.drawBorder(x, y, 16, 16, rarityColor)
            }

            3 -> ctx.drawTexture(circleTexture, x, y, 16, 16, color)
        }
    }
}