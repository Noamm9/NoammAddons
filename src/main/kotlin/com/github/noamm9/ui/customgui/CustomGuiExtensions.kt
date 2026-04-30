package com.github.noamm9.ui.customgui

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot

var AbstractContainerScreen<*>.customGui: CustomGui?
    get() = (this as IHasCustomGui).noammaddons_getCustomGui()
    set(value) = (this as IHasCustomGui).noammaddons_setCustomGui(value)

val Slot.originalX: Int get() = (this as ICoordRememberingSlot).noammaddons_getOriginalX()
val Slot.originalY: Int get() = (this as ICoordRememberingSlot).noammaddons_getOriginalY()

fun Slot.rememberCoords() = (this as ICoordRememberingSlot).noammaddons_rememberCoords()
fun Slot.restoreCoords() = (this as ICoordRememberingSlot).noammaddons_restoreCoords()

fun Slot.setSlotX(x: Int) = (this as ICoordRememberingSlot).noammaddons_setX(x)
fun Slot.setSlotY(y: Int) = (this as ICoordRememberingSlot).noammaddons_setY(y)
