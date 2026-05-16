package com.github.noamm9.features.impl.general.storageoverlay

import java.util.*

internal data class StorageData(val storageInventories: SortedMap<StoragePage, StorageInventory> = TreeMap()) {
    data class StorageInventory(var title: String, val slot: StoragePage, var inventory: VirtualInventory?)
}