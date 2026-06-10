package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.FastFill
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import java.awt.Color
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull


object FEAT_ItemRarity: Feature(name = "Item Rarity", description = "Draws the rarity of item behind the slot.") {
    @JvmStatic val drawOnHotbar by ToggleSetting("Draw on Hotbar", true)
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
        // Build the ARGB int directly instead of allocating a Color per item per frame (hot path in storage overlays).
        val base = rarity.color
        val alpha = (255 * rarityOpacity.value / 100f).toInt().coerceIn(0, 255)
        val argb = (alpha shl 24) or (base.rgb and 0xFFFFFF)

        when (style.value) {
            0 -> ctx.fill(x, y, x + 16, y + 16, argb)
            1 -> Render2D.drawBorder(ctx, x, y, 16, 16, Color(argb, true))
            2 -> {
                ctx.fill(x, y, x + 16, y + 16, argb)
                Render2D.drawBorder(ctx, x, y, 16, 16, base)
            }
        }
    }

    /**
     * Same as [onSlotDraw] but routes the rects into [FastFill] instead of issuing individual `GuiGraphics.fill`
     * calls. Used by the storage overlay, which draws hundreds of slots per frame - batching keeps it to a single
     * render state. Caller is responsible for flushing the batch.
     */
    @JvmStatic
    fun drawRarity(stack: ItemStack?, x: Int, y: Int) {
        if (stack == null) return
        drawRarity(ItemUtils.getRarity(stack), x, y)
    }

    /** Like [drawRarity] above but takes an already-resolved [rarity] (the storage overlay pre-computes it per page). */
    @JvmStatic
    fun drawRarity(rarity: ItemRarity, x: Int, y: Int) {
        if (! LocationUtils.inSkyblock) return
        if (rarity == ItemRarity.NONE) return
        val base = rarity.color
        val alpha = (255 * rarityOpacity.value / 100f).toInt().coerceIn(0, 255)
        val argb = (alpha shl 24) or (base.rgb and 0xFFFFFF)

        when (style.value) {
            0 -> FastFill.add(x, y, x + 16, y + 16, argb)
            1 -> FastFill.addBorder(x, y, 16, 16, 1, argb)
            2 -> {
                FastFill.add(x, y, x + 16, y + 16, argb)
                FastFill.addBorder(x, y, 16, 16, 1, base.rgb)
            }
        }
    }
}