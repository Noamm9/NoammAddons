package com.github.noamm9.features.impl.general.storageoverlay

internal interface ICoordRememberingSlot {
    fun noammaddons_rememberCoords()
    fun noammaddons_restoreCoords()
    fun noammaddons_setX(x: Int)
    fun noammaddons_setY(y: Int)
}
