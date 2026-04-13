package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.utils.ChatUtils

data class StoragePageSlot(val index: Int) : Comparable<StoragePageSlot> {
    val isEnderChest get() = index < 9
    val isBackPack get() = !isEnderChest
    val slotIndexInOverviewPage get() = if (isEnderChest) index + 9 else index + 18

    fun defaultName() = if (isEnderChest) "Ender Chest #${index + 1}" else "Backpack #${index - 9 + 1}"

    fun navigateTo() = ChatUtils.sendCommand(if (isBackPack) "backpack ${index - 9 + 1}" else "enderchest ${index + 1}")

    companion object {
        fun fromOverviewSlotIndex(slot: Int) = when (slot) { in 9 until 18 -> StoragePageSlot(slot - 9); in 27 until 45 -> StoragePageSlot(slot - 27 + 9); else -> null }

        fun ofEnderChestPage(slot: Int) = StoragePageSlot(slot - 1)
        fun ofBackPackPage(slot: Int) = StoragePageSlot(slot - 1 + 9)
    }

    override fun compareTo(other: StoragePageSlot) = this.index - other.index
}
