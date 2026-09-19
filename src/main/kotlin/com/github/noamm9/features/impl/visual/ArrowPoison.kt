package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.uppercaseFirst
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object ArrowPoison: Feature("Shows on screen the amount of poison arrows you have in inventory") {
    override fun init() {
        hudElement(name, shouldDraw = { PoisonArrow.entries.any { it.count > 0 } }) { ctx, e ->
            val arrows = if (e) PoisonArrow.entries else PoisonArrow.entries.filter { it.count > 0 }

            var width = 0
            var height = 0

            for (arrow in arrows) {
                val text = arrow.displayName + ": &f${arrow.count}"

                ctx.item(arrow.previewItem.value, 0, height - 1)
                ctx.drawString(text, 17, height + 4.5, color = arrow.color)
                width = maxOf(width, 18 + text.width())

                height += 16
            }

            return@hudElement width to height
        }

        register<MainThreadPacketReceivedEvent.Post> {
            val packet = event.packet as? ClientboundContainerSetSlotPacket ?: return@register
            if (packet.containerId != 0) return@register
            val arrow = PoisonArrow.entries.find { it.sbid == packet.item.skyblockId } ?: return@register
            arrow.count = player.inventory.nonEquipmentItems.sumOf {
                it.takeIf { it.skyblockId == arrow.sbid }?.count ?: 0
            }
        }

        ThreadUtils.loop(1000) {
            if (enabled && mc.player != null) mc.execute {
                PoisonArrow.entries.forEach { arrow ->
                    arrow.count = player.inventory.nonEquipmentItems.sumOf {
                        it.takeIf { it.skyblockId == arrow.sbid }?.count ?: 0
                    }
                }
            }
        }

        register<WorldChangeEvent> { PoisonArrow.reset() }
    }

    private enum class PoisonArrow(val previewItem: Lazy<ItemStack>, val sbid: String, val color: Color) {
        TWILIGHT(lazy { Items.DYE.purple().defaultInstance }, "TWILIGHT_ARROW_POISON", Color.MAGENTA),
        TOXIC(lazy { Items.DYE.lime().defaultInstance }, "TOXIC_ARROW_POISON", Color.GREEN);

        val displayName = name.lowercase().uppercaseFirst()
        var count = 0

        companion object {
            fun reset() = entries.forEach { it.count = 0 }
        }
    }
}