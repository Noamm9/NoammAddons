package com.github.noamm9.ui.customgui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.inventory.Slot

abstract class CustomGui {

    open fun render(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {}

    open fun mouseClick(click: MouseButtonEvent, doubled: Boolean): Boolean = false

    open fun mouseReleased(click: MouseButtonEvent): Boolean = false

    open fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean = false

    open fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = false

    open fun keyPressed(input: KeyEvent): Boolean = false

    open fun keyReleased(input: KeyEvent): Boolean = false

    open fun charTyped(input: CharacterEvent): Boolean = false

    open fun moveSlot(slot: Slot) {}

    open fun beforeSlotRender(context: GuiGraphics, slot: Slot) {}

    open fun afterSlotRender(context: GuiGraphics, slot: Slot) {}

    open fun onInit() {}

    open fun shouldDrawForeground(): Boolean = true

    open fun onVoluntaryExit(): Boolean = true

    open fun isClickOutsideBounds(mouseX: Double, mouseY: Double): Boolean = false

    open fun isPointOverSlot(slot: Slot, xO: Int, yO: Int, pX: Double, pY: Double) = pX >= slot.x + xO && pX < slot.x + xO + 16 && pY >= slot.y + yO && pY < slot.y + yO + 16
}
