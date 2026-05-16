package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.utils.ChatUtils

internal data class StoragePage(val index: Int): Comparable<StoragePage> {
    val isEnderChest = index < 9
    val isBackPack = ! isEnderChest

    fun defaultName() = if (isEnderChest) "Ender Chest #${index + 1}" else "Backpack #${index - 9 + 1}"
    fun navigateTo() = ChatUtils.sendCommand(if (isBackPack) "backpack ${index - 9 + 1}" else "enderchest ${index + 1}")

    override fun compareTo(other: StoragePage) = this.index - other.index

    companion object {
        fun fromOverviewSlotIndex(slot: Int) = when (slot) {
            in 9 until 18 -> StoragePage(slot - 9)
            in 27 until 45 -> StoragePage(slot - 27 + 9)
            else -> null
        }

        fun ofEnderChestPage(slot: Int) = StoragePage(slot - 1)
        fun ofBackPackPage(slot: Int) = StoragePage(slot - 1 + 9)
    }
}