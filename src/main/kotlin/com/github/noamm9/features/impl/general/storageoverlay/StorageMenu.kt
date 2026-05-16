package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.utils.ChatUtils.unformattedText
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu

sealed interface StorageMenu {
    private sealed interface Menu {
        val handler: ChestMenu
    }

    data class Overview(override val handler: ChestMenu): StorageMenu, Menu
    data class Page(override val handler: ChestMenu, val storagePage: StoragePage): StorageMenu, Menu

    companion object {
        private val enderChestName = "^Ender Chest (?:✦ )?\\(([1-9])/[1-9]\\)$".toRegex()
        private val backPackName = "^.+Backpack (?:✦ )?\\(Slot #([0-9]+)\\)$".toRegex()

        fun fromScreen(screen: Screen?): StorageMenu? {
            if (screen == null || screen !is ContainerScreen) return null
            val title = screen.title.unformattedText
            if (title == "Storage") return Overview(screen.menu)
            enderChestName.matchEntire(title)?.let { return Page(screen.menu, StoragePage.ofEnderChestPage(it.groupValues[1].toInt())) }
            backPackName.matchEntire(title)?.let { return Page(screen.menu, StoragePage.ofBackPackPage(it.groupValues[1].toInt())) }
            return null
        }
    }
}