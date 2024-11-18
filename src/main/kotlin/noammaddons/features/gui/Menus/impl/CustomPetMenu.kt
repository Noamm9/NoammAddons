package noammaddons.features.gui.Menus.impl

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemSkull
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiContainerEvent
import noammaddons.features.Feature
import noammaddons.features.gui.Menus.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderUtils.drawGradientRoundedRect
import noammaddons.utils.RenderUtils.drawTextWithoutColorLeak
import noammaddons.utils.RenderUtils.drawWithNoLeak
import noammaddons.utils.RenderUtils.renderItem
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.isNull
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.floor

object CustomPetMenu: Feature() {
    val petMenuRegex = Regex("Pets( \\(\\d/\\d\\) )?") // https://regex101.com/r/wQn9e4/2
    private val inPetMenu: Boolean get() = currentChestName.removeFormatting().matches(petMenuRegex) && config.CustomSBMenus
    private val PetSlots = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    )

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (! inPetMenu) return
        if (! event.button.equalsOneOf(0, 1, 2)) return
        val container = Player?.openContainer?.inventorySlots ?: return
        event.isCanceled = true

        val scale = calculateScale()
        val (x, y) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        val (offsetX, offsetY, _, _) = calculateOffsets(screenSize, windowSize)
        val slotPosition = calculateSlotPosition(x, y, offsetX, offsetY)

        if (! isValidSlot(slotPosition.first, slotPosition.second)) return
        val slot = getSlotIndex(slotPosition.first, slotPosition.second)

        if (slot >= windowSize) return
        container[slot].run {
            if (stack.isNull()) return
            if (stack.getItemId() == 160 && stack.metadata == 15) return
        }

        handleSlotClick(event.button, slot)

        if (slot in PetSlots && ! Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            closeScreen()
        }
    }


    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inPetMenu) return
        event.isCanceled = true
        val container = Player?.openContainer?.inventorySlots ?: return

        val scale = calculateScale()
        val (mx, my) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        val (offsetX, offsetY, width, height) = calculateOffsets(screenSize, windowSize)
        val slotPosition = calculateSlotPosition(mx, my, offsetX, offsetY)

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)

        renderBackground(offsetX, offsetY, width, height, backgroundColor)

        drawTextWithoutColorLeak(
            "&6&l&n[&b&l&nN&d&l&nA&6&l&n]&r &b&lPet Menu".addColor(),
            offsetX, offsetY, 1f, Color.WHITE
        )

        for (slot in container) {
            val i = slot !!.slotNumber
            if (i >= windowSize) continue
            if (i < 7) continue
            if (slot.stack.isNull()) continue
            if (slot.stack.getItemId() == 160) continue

            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor(i / 9.0).toInt() * 18 + offsetY

            if (slot.stack.lore.joinToString().removeFormatting().contains("Click to despawn!")) drawWithNoLeak {
                drawGradientRoundedRect(
                    currentOffsetX + 0.5f, currentOffsetY + 0.5f,
                    15f, 15f, 1.5f,
                    getRainbowColor(0f),
                    getRainbowColor(0.33f),
                    getRainbowColor(1f),
                    getRainbowColor(0.66f),
                )
            }
        }

        renderHeads(container, windowSize, offsetX, offsetY, slotPosition, 0)

        container.forEach { slot ->
            val i = slot.slotNumber
            val stack = slot.stack
            if (i < 7) return@forEach
            if (i >= windowSize) return@forEach
            if (stack.isNull()) return@forEach
            if (stack.item is ItemSkull) return@forEach
            if (stack.getItemId() == 160 && stack.metadata == 15) return@forEach

            renderItem(
                stack,
                i % 9f * 18f + offsetX,
                floor(i / 9.0).toInt() * 18f + offsetY
            )
        }

        GlStateManager.popMatrix()

        if (! isValidSlot(slotPosition.first, slotPosition.second)) return
        val slotIndex = getSlotIndex(slotPosition.first, slotPosition.second)
        if (slotIndex >= windowSize) return

        val item = container[slotIndex]?.stack ?: return
        if (item.getItemId() == 160 && item.metadata == 15) return

        updateLastSlot(slotIndex)
        drawLore(item.displayName, item.lore, mx, my, scale, screenSize)
    }
}