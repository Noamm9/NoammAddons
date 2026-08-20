package com.github.noamm9.features.impl.general

import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.config.types.KeybindConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.itemUUID
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import kotlin.jvm.optionals.getOrDefault

object ProtectItem: Feature("Prevents dropping or selling important items via /protectitem or keybind."), ICommandProvider {
    private var data = PogObject("item_protection", object {
        val uuids = mutableSetOf<String>()
        val ids = mutableSetOf<String>()
    })

    private val protectNodification by BooleanConfig("Protect Notification", true).withDescription("Shows a notification on the bottom right side of the screen when the feature saved your item")
    private val protectBind by KeybindConfig("Protect Key", GLFW.GLFW_KEY_L).section("Keybind").withDescription("Press while hovering an item in an inventory to protect/unprotect it via UUID.")
    private val showProtected by BooleanConfig("Show Protected Items").withDescription("Shows protected items in container GUIs with a small indicator.")
    private val protectUUID by BooleanConfig("Protect UUID", true)
    private val protectID by BooleanConfig("Protect Skyblock ID", true)
    private val protectStarred by BooleanConfig("Protect Starred", true)
    private val protectRarity by BooleanConfig("Protect Recombobulated", true)

    override fun init() {
        register<ContainerEvent.SlotClick> {
            if (! enabled) return@register
            val menu = player.containerMenu

            val stack = when (event.slotId) {
                - 999 -> menu.carried
                in menu.slots.indices -> menu.slots[event.slotId].item
                else -> ItemStack.EMPTY
            }

            if (stack.isEmpty) return@register

            val isThrowing = event.clickType == ContainerInput.THROW || event.slotId == - 999
            val isSelling = isSellMenu() && event.slotId in menu.slots.indices

            if (isThrowing || isSelling) {
                if (getProtectType(stack) != ProtectType.None) {
                    if (protectNodification.value) NotificationManager.push("Action Blocked", "This item is protected!", 1500L)
                    event.isCanceled = true
                }
            }

            if (protectBind.isDown()) {
                event.isCanceled = true
                protect(stack)
            }
        }

        register<KeyboardEvent.KeyPressed> {
            if (LocationUtils.inDungeon && DungeonListener.dungeonStarted && ! DungeonListener.dungeonEnded) return@register
            if (mc.screen != null) return@register
            if (! mc.options.keyDrop.matches(event.keyEvent)) return@register

            if (getProtectType(player.inventory.selectedItem) != ProtectType.None) {
                if (protectNodification.value) NotificationManager.push("Action Blocked", "This item is protected!", 1500L)
                event.isCanceled = true
            }
        }

        register<ContainerEvent.Render.Tooltip> {
            if (event.stack.isEmpty) return@register
            if (event.lore.isEmpty()) return@register
            val type = getProtectType(event.stack)
            if (type != ProtectType.None) {
                event.lore.add(1, Component.literal("§aItem Protected §7(${type.name})"))
            }
        }

        register<ContainerEvent.Render.Slot.Post> {
            if (! showProtected.value) return@register
            val stack = event.slot.item.takeUnless { it.isEmpty } ?: return@register
            if (getProtectType(stack) != ProtectType.None) {
                val x = event.slot.x + 1
                val y = event.slot.y + 1
                event.context.drawString("§aP", x, y, scale = 0.75)
            }
        }
    }

    override fun CommandBuilder.command() {
        setName("protectitem")
        runs {
            val heldItem = player.mainHandItem.takeUnless { it.isEmpty }
                ?: return@runs ChatUtils.modMessage("&cYou need to be holding an item.")
            protect(heldItem)
        }
    }

    private fun getProtectType(stack: ItemStack): ProtectType {
        if (stack.isEmpty) return ProtectType.None

        if (protectUUID.value) {
            val uuid = stack.itemUUID
            if (uuid.isNotBlank() && uuid in data.get().uuids) return ProtectType.UUID
        }

        if (protectID.value) {
            val id = stack.skyblockId
            if (id.isNotBlank() && id in data.get().ids) return ProtectType.SkyblockID
        }

        val data = stack.customData
        val name = stack.hoverName.unformattedText
        if (protectStarred.value && (data.getInt("upgrade_level").getOrDefault(0) > 0 || name.contains("✪"))) return ProtectType.Starred
        if (protectRarity.value && data.getInt("rarity_upgrades").getOrDefault(0) > 0) return ProtectType.RarityUpgraded

        return ProtectType.None
    }

    private fun isSellMenu(): Boolean {
        return player.containerMenu.slots.take(54).any { slot ->
            if (slot.item.isEmpty) return@any false

            val isHopper = slot.item.`is`(Items.HOPPER) && slot.item.hoverName.string.contains("Sell Item")
            val hasBuyback = slot.item.lore.any { it.contains("Click to buyback") }

            isHopper || hasBuyback
        }
    }

    private fun protect(stack: ItemStack) {
        val label = stack.hoverName.formattedText
        val data = data.get()

        val (list, id) = when {
            stack.itemUUID.isNotBlank() -> data.uuids to stack.itemUUID
            stack.skyblockId.isNotBlank() -> data.ids to stack.skyblockId
            else -> return NotificationManager.push("Error", "Item has no protectable ID/UUID.")
        }

        if (list.remove(id)) NotificationManager.push("&cProtection Removed", "No longer protecting $label.")
        else {
            list.add(id)
            NotificationManager.push("&aProtection Added", "Now protecting $label.")
        }
    }

    private enum class ProtectType { UUID, SkyblockID, Starred, RarityUpgraded, None }
}