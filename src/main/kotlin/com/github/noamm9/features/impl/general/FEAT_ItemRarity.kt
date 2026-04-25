package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull


object FEAT_ItemRarity: Feature(name = "Item Rarity", description = "Draws the rarity of item behind the slot.") {
    val drawOnHotbar by ToggleSetting("Draw on Hotbar", true)
    private val rarityOpacity by SliderSetting("Rarity Opacity", 30f, 10f, 100f, 1f)
    private val style by DropdownSetting("Rarity Style", 0, listOf("Filled", "Outline", "Filled Outline"))

    private val baseStatBoost by ToggleSetting("Show Item Quality", true).section("Lore")
        .withDescription("Shows the base stats boost of dungeon items as well as the floor they were dropped on")

    override fun init() {
        register<ContainerEvent.Render.Slot.Pre> {
            onSlotDraw(event.context, event.slot.item, event.slot.x, event.slot.y)
        }

        register<ContainerEvent.Render.Tooltip> {
            if (! baseStatBoost.value) return@register
            if (! LocationUtils.inSkyblock) return@register
            val data = event.stack.customData.takeUnless { it == CustomData.EMPTY } ?: return@register
            val boost = data.getInt("baseStatBoostPercentage").getOrNull()?.takeIf { it > 0 } ?: return@register
            val req = data.getString("dungeon_skill_req").getOrDefault("")
            val tier = data.getInt("item_tier").getOrDefault(0)

            val floor = when {
                req.isEmpty() && tier > 0 -> "§aE"
                req.isEmpty() -> "§bF$tier"
                else -> {
                    val (dungeon, level) = req.split(':', limit = 2)
                    val levelReq = level.toIntOrNull() ?: 0
                    if (dungeon == "CATACOMBS") {
                        if (levelReq - tier > 19) {
                            "§4M${tier - 3}"
                        }
                        else "§aF$tier"
                    }
                    else "§b${dungeon} $tier"
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
    }

    /**
     * @see com.github.noamm9.mixin.MixinGui
     */
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