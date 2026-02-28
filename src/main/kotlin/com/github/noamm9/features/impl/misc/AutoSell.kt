package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.TextInputSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.Utils.equalsOneOf
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.entity.player.Inventory

object AutoSell: Feature("Automatically sell useless items for you.") {
    private val itemsToSell by TextInputSetting("Items to Sell", "").withDescription("Enter item names separated by commas")
    private val baseDelay by SliderSetting("Base Delay", 6, 2, 10, 1).withDescription("Ticks between sell actions").also { it.headerName = "Configuration" }
    private val delayVariance by SliderSetting("Delay Variance", 1, 0, 5, 1).withDescription("Random variance in ticks")
    private val mouseButton by DropdownSetting("Mouse Button", 0, listOf("Shift-Click", "Middle-Click", "Left-Click")).withDescription("Which mouse button to use")
    private val populateDefaults by ButtonSetting("Load Default Items") {
        val existingItems = itemsToSell.value.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        existingItems.addAll(getDefaultSellItems())
        itemsToSell.value = existingItems.joinToString(", ")
        modMessage("§aDefault items loaded")
    }

    private var nextActionTime = 0L
    private var itemBlacklist = setOf<String>()

    override fun init() {
        register<TickEvent.Server> {
            if (!enabled) return@register

            updateItemBlacklist()

            if (itemBlacklist.isEmpty()) return@register
            if (System.currentTimeMillis() < nextActionTime) return@register

            performSell()
        }
        nextActionTime = System.currentTimeMillis()
    }

    private fun updateItemBlacklist() {
        itemBlacklist = itemsToSell.value
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun performSell() {
        val gameMode = mc.gameMode ?: return
        val player = mc.player ?: return
        val screen = mc.screen

        if (screen !is AbstractContainerScreen<*>) return

        val screenTitle = screen.title?.string ?: return
        if (!isSellableContainer(screenTitle)) return

        val targetSlot = findItemToSell(screen) ?: return

        val clickType = resolveClickType(mouseButton.value)
        gameMode.handleInventoryMouseClick(screen.menu.containerId, targetSlot, 0, clickType, player)

        scheduleNextAction()
    }

    private fun isSellableContainer(name: String): Boolean {
        return name.equalsOneOf("Trades", "Booster Cookie", "Farm Merchant", "Ophelia")
    }

    private fun findItemToSell(container: AbstractContainerScreen<*>): Int? {
        return container.menu.slots
            .filter { it.container is Inventory }
            .firstOrNull { slot ->
                val item = slot.item ?: return@firstOrNull false
                if (item.isEmpty) return@firstOrNull false

                val name = item.hoverName?.string?.removeFormatting()?.lowercase() ?: return@firstOrNull false
                itemBlacklist.any { name.contains(it) }
            }
            ?.index
    }

    private fun resolveClickType(buttonIndex: Int): ClickType = when (buttonIndex) {
        1 -> ClickType.CLONE
        2 -> ClickType.PICKUP
        else -> ClickType.QUICK_MOVE
    }

    private fun scheduleNextAction() {
        val jitter = if (delayVariance.value > 0) (0..delayVariance.value.toInt()).random() else 0
        val totalDelay = baseDelay.value.toInt() + jitter
        nextActionTime = System.currentTimeMillis() + (totalDelay * 50)
    }

    private fun getDefaultSellItems() = listOf(
        "enchanted ice", "rotten", "skeleton grunt", "cutlass",
        "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
        "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
        "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
        "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
        "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
        "healing viii splash potion", "healing 8 splash potion", "candycomb", "conjuring"
    )
}