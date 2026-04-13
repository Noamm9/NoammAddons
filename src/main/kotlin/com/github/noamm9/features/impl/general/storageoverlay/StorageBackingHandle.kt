package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.utils.ChatUtils.unformattedText
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu

sealed interface StorageBackingHandle {

    sealed interface HasBackingScreen {
        val handler: ChestMenu
    }

    data class Overview(override val handler: ChestMenu) : StorageBackingHandle, HasBackingScreen

    data class Page(override val handler: ChestMenu, val storagePageSlot: StoragePageSlot) : StorageBackingHandle, HasBackingScreen

    companion object {
        private val enderChestName = "^Ender Chest (?:✦ )?\\(([1-9])/[1-9]\\)$".toRegex()
        private val backPackName = "^.+Backpack (?:✦ )?\\(Slot #([0-9]+)\\)$".toRegex()

        fun fromScreen(screen: Screen?): StorageBackingHandle? {
            if (screen == null) return null
            if (screen !is ContainerScreen) return null
            val title = screen.title.unformattedText
            if (title == "Storage") return Overview(screen.menu)

            enderChestName.matchEntire(title)?.let { return Page(screen.menu, StoragePageSlot.ofEnderChestPage(it.groupValues[1].toInt())) }
            backPackName.matchEntire(title)?.let { return Page(screen.menu, StoragePageSlot.ofBackPackPage(it.groupValues[1].toInt())) }
            return null
        }
    }
}
