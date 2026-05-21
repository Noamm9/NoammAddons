package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.utils.ChatUtils.unformattedText
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu

abstract class StorageMenu {
    abstract val handler: ChestMenu

    class Overview(override val handler: ChestMenu): StorageMenu()
    class Page(override val handler: ChestMenu, val storagePage: StoragePage): StorageMenu()

    companion object {
        private val enderChestRegex = Regex("^Ender Chest (?:✦ )?\\(([1-9])/[1-9]\\)$")
        private val backPackRegex = Regex("^.+Backpack (?:✦ )?\\(Slot #([0-9]+)\\)$")

        fun get(screen: Screen?): StorageMenu? {
            if (screen == null || screen !is ContainerScreen) return null
            val title = screen.title.unformattedText
            if (title == "Storage") return Overview(screen.menu)
            enderChestRegex.find(title)?.destructured?.component1()?.let { return Page(screen.menu, StoragePage.enderchest(it.toInt())) }
            backPackRegex.find(title)?.destructured?.component1()?.let { return Page(screen.menu, StoragePage.backpack(it.toInt())) }
            return null
        }
    }
}