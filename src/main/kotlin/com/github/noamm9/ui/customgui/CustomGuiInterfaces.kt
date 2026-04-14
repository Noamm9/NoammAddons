package com.github.noamm9.ui.customgui

interface IHasCustomGui {
    fun noammaddons_getCustomGui(): CustomGui?
    fun noammaddons_setCustomGui(gui: CustomGui?)
}

interface ICoordRememberingSlot {
    fun noammaddons_rememberCoords()
    fun noammaddons_restoreCoords()
    fun noammaddons_getOriginalX(): Int
    fun noammaddons_getOriginalY(): Int
    fun noammaddons_setX(x: Int)
    fun noammaddons_setY(y: Int)
}
