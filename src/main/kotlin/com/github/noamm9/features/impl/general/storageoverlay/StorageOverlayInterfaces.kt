package com.github.noamm9.features.impl.general.storageoverlay

internal interface IStorageOverlayHolder {
    fun noammaddons_getStorageOverlay(): StorageOverlayCustom?
    fun noammaddons_setStorageOverlay(gui: StorageOverlayCustom?)
}

internal interface ICoordRememberingSlot {
    fun noammaddons_rememberCoords()
    fun noammaddons_restoreCoords()
    fun noammaddons_setX(x: Int)
    fun noammaddons_setY(y: Int)
}
