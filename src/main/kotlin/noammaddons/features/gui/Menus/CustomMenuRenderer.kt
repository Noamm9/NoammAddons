package noammaddons.features.gui.Menus

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiMouseClickEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.isNull

object CustomMenuRenderer: Feature() {

    @SubscribeEvent
    fun onClick(event: GuiMouseClickEvent) {
        if (! inMenu()) return
        if (! event.button.equalsOneOf(0, 1, 2)) return

        val container = Player?.openContainer?.inventorySlots ?: return
        event.isCanceled = true

        val scale = calculateScale()
        val expend = shouldExpand(container)
        val (x, y) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        var (offsetX, offsetY, _, _) = calculateOffsets(screenSize, windowSize)
        if (expend == 1) offsetY += 9
        val slotPosition = calculateSlotPosition(x, y, offsetX, offsetY)

        if (! isValidSlot(slotPosition.first, slotPosition.second)) return
        val slot = getSlotIndex(slotPosition.first, slotPosition.second)

        debugMessage("Slot: ${slot}, x: ${slotPosition.first}, y: ${slotPosition.second}")

        if (slot >= windowSize) return
        container[slot].run {
            if (stack.isNull()) return
            if (stack.getItemId() == 160 && stack.metadata == 15) return
        }

        handleSlotClick(event.button, slot)
    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inMenu()) return
        event.isCanceled = true

        val container = Player?.openContainer?.inventorySlots ?: return
        val expend = shouldExpand(container)

        val scale = calculateScale()
        val (mx, my) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        var (offsetX, offsetY, width, height) = calculateOffsets(screenSize, windowSize)
        if (expend == 1) {
            height += 18
            offsetY -= 9
        }
        val slotPosition = calculateSlotPosition(mx, my, offsetX, offsetY)

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)

        renderBackground(offsetX, offsetY, width, height, backgroundColor)
        renderMenuTitle(currentChestName, offsetX, offsetY)

        renderHeads(container, windowSize, offsetX, offsetY, slotPosition, expend)
        renderItems(container, windowSize, offsetX, offsetY, expend)

        GlStateManager.popMatrix()

        val slotY = slotPosition.second - expend
        if (! isValidSlot(slotPosition.first, slotY)) return
        val slotIndex = getSlotIndex(slotPosition.first, slotY)
        if (slotIndex >= windowSize) return

        val item = container[slotIndex]?.stack ?: return
        if (isBackgroundGlass(item)) return

        updateLastSlot(slotIndex)
        drawLore(item.displayName, item.lore, mx, my, scale, screenSize)
    }
}