package com.github.noamm9.features.impl.general

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.itemUUID
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

object ProtectItem: Feature("Prevents dropping or selling important items. /protectitem") {
    private val data = PogObject("item_protection", mutableMapOf<String, List<String>>(
        "uuids" to listOf(),
        "ids" to listOf()
    ))

    private val protectUUID by ToggleSetting("Protect UUID", true)
    private val protectID by ToggleSetting("Protect Skyblock ID", true)
    private val protectStarred by ToggleSetting("Protect Starred", true)
    private val protectRarity by ToggleSetting("Protect Recombobulated", true)

    override fun init() {
        register<ContainerEvent.SlotClick> {
            if (! enabled) return@register
            val menu = mc.player?.containerMenu ?: return@register

            val stack = when (event.slotId) {
                - 999 -> menu.carried
                in 0 until menu.slots.size -> menu.slots[event.slotId].item
                else -> ItemStack.EMPTY
            }

            if (stack.isEmpty) return@register

            val isThrowing = event.clickType == ClickType.THROW || event.slotId == - 999
            val isSelling = isSellMenu() && event.slotId in 0 until menu.slots.size

            if (isThrowing || isSelling) {
                if (getProtectType(stack) != ProtectType.None) {
                    NotificationManager.push("Action Blocked", "This item is protected!", 1500L)
                    event.isCanceled = true
                }
            }
        }

        register<KeyboardEvent.KeyPressed> {
            if (! enabled || mc.screen != null) return@register
            if (LocationUtils.inDungeon) return@register

            val dropKey = (mc.options.keyDrop as? IKeyMapping)?.key?.value ?: - 1
            if (event.keyEvent.key != dropKey || event.keyEvent.scancode != GLFW.GLFW_PRESS) return@register

            val heldItem = mc.player?.inventory?.selectedItem ?: return@register
            if (getProtectType(heldItem) != ProtectType.None) {
                NotificationManager.push("Action Blocked", "This item is protected!", 1500L)
                event.isCanceled = true
            }
        }

        register<ContainerEvent.Render.Tooltip> {
            if (event.stack.isEmpty) return@register
            val type = getProtectType(event.stack)
            if (type != ProtectType.None) {
                event.lore.add(1, Component.literal("§aItem Protected §7(${type.name})"))
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommandManager.literal("protectitem").executes {
                val heldItem = mc.player?.inventory?.selectedItem?.takeUnless { it.isEmpty } ?: run {
                    ChatUtils.modMessage("&cYou need to be holding an item.")
                    return@executes 1
                }

                if (heldItem.itemUUID.isNotBlank()) toggle(heldItem.itemUUID, "uuids", heldItem.hoverName.formattedText)
                else if (heldItem.skyblockId.isNotBlank()) toggle(heldItem.skyblockId, "ids", heldItem.hoverName.formattedText)
                else NotificationManager.push("Error", "Item has no unique data.")

                1
            })
        }
    }

    private fun getProtectType(stack: ItemStack): ProtectType {
        if (stack.isEmpty) return ProtectType.None

        if (protectUUID.value) {
            val uuid = stack.itemUUID
            if (uuid.isNotBlank() && data.getData()["uuids"] !!.contains(uuid)) return ProtectType.UUID
        }

        if (protectID.value) {
            val id = stack.skyblockId
            if (id.isNotBlank() && data.getData()["ids"] !!.contains(id)) return ProtectType.SkyblockID
        }

        val data = stack.customData
        val name = stack.hoverName.unformattedText
        if (protectStarred.value && (data.getInt("upgrade_level").orElse(0) > 0 || name.contains("✪"))) return ProtectType.Starred
        if (protectRarity.value && data.getInt("rarity_upgrades").orElse(0) > 0) return ProtectType.RarityUpgraded

        return ProtectType.None
    }

    private fun isSellMenu(): Boolean {
        val menu = mc.player?.containerMenu ?: return false
        return menu.slots.take(54).any { slot ->
            if (slot.item.isEmpty) return@any false

            val isHopper = slot.item.`is`(Items.HOPPER) && slot.item.hoverName.string.contains("Sell Item")
            val hasBuyback = slot.item.lore.any { it.contains("Click to buyback") }

            isHopper || hasBuyback
        }
    }

    private fun toggle(id: String, key: String, label: String) {
        val data = data.getData()
        val set = data[key]?.toMutableSet() ?: return

        if (set.contains(id)) {
            set.remove(id)
            NotificationManager.push("&cProtection Removed", "No longer protecting $label.")
        }
        else {
            set.add(id)
            NotificationManager.push("&aProtection Added", "Now protecting $label.")
        }

        data[key] = set.toList()
        this.data.save()
    }

    private enum class ProtectType { UUID, SkyblockID, Starred, RarityUpgraded, None }
}