package com.github.noamm9.features.impl.general.storageoverlay

import java.util.SortedMap
import java.util.TreeMap

data class StorageData(val storageInventories: SortedMap<StoragePageSlot, StorageInventory> = TreeMap()) {
    data class StorageInventory(var title: String, val slot: StoragePageSlot, var inventory: VirtualInventory?)
}
