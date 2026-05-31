package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.Slot as McSlot

abstract class ContainerEvent(val screen: AbstractContainerScreen<*>): Event(cancelable = true) {
    abstract class Render(screen: Screen, val context: GuiGraphicsExtractor): ContainerEvent(screen as AbstractContainerScreen<*>) {
        abstract class Slot(screen: Screen, ctx: GuiGraphicsExtractor, val slot: McSlot): Render(screen, ctx) {
            class Pre(screen: Screen, ctx: GuiGraphicsExtractor, slot: McSlot): Slot(screen, ctx, slot)
            class Post(screen: Screen, ctx: GuiGraphicsExtractor, slot: McSlot): Slot(screen, ctx, slot)
        }

        class Tooltip(screen: Screen, ctx: GuiGraphicsExtractor, val stack: ItemStack, val mouseX: Int, val mouseY: Int, val lore: MutableList<Component>): Render(screen, ctx)
    }

    class Open(screen: Screen): ContainerEvent(screen as AbstractContainerScreen<*>)
    class Close(screen: Screen): ContainerEvent(screen as AbstractContainerScreen<*>)

    class SlotClick(screen: Screen, val slotId: Int, val button: Int, val clickType: ContainerInput): ContainerEvent(screen as AbstractContainerScreen<*>)
    class MouseClick(screen: Screen, val mouseX: Double, val mouseY: Double, val button: Int, val modifiers: Int): ContainerEvent(screen as AbstractContainerScreen<*>)
    class MouseScroll(screen: Screen, val mouseX: Double, val mouseY: Double, val horizontalAmount: Double, val verticalAmount: Double): ContainerEvent(screen as AbstractContainerScreen<*>)

    class Keyboard(screen: Screen, val key: Int, val input: Char, val scancode: Int, val modifiers: Int): ContainerEvent(screen as AbstractContainerScreen<*>)
}