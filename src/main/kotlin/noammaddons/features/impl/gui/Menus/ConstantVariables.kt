package noammaddons.features.impl.gui.Menus

import net.minecraft.inventory.Slot
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.client.config.GuiUtils.*
import noammaddons.features.impl.gui.CustomMenuSettings
import noammaddons.features.impl.gui.ScalableTooltips
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.ItemUtils.getHeadSkinTexture
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.MouseUtils.getMouseX
import noammaddons.utils.MouseUtils.getMouseY
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawPlayerHead
import noammaddons.utils.RenderUtils.drawRainbowRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.renderItem
import noammaddons.utils.SoundUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.floor


/*
    This file contains variables and functions


    Notes for myself:

    Gui's items must be rendered before the skulls
    because the function messes up with the
    matrix stack and I have no clue why

    Tooltip must be rendered outside the scaling
*/

var lastSlot: Int? = null
val backgroundColor = Color(33, 33, 33, 195)


fun isValidSlot(slotX: Int, slotY: Int): Boolean = slotX in 0 .. 8 && slotY >= 0
fun getSlotIndex(slotX: Int, slotY: Int): Int = slotX + slotY * 9

fun updateLastSlot(slotIndex: Int) {
    if (lastSlot == null) lastSlot = slotIndex
    if (lastSlot != slotIndex) {
        lastSlot = slotIndex
        ScalableTooltips.resetPos()
    }
}

fun handleSlotClick(button: Int, slotIndex: Int) {
    sendWindowClickPacket(
        slotIndex, button,
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) 1 else 0
    )

    SoundUtils.click()
}

fun calculateScale(): Float {
    return (7f * (CustomMenuSettings.scale.toFloat() / 100f)) / mc.getScaleFactor()
}

fun getMouseScaledCoordinates(scale: Float): Pair<Float, Float> {
    return Pair(
        getMouseX() / scale,
        getMouseY() / scale
    )
}

fun getScreenSize(scale: Float): Pair<Float, Float> {
    return Pair(
        mc.getWidth() / scale,
        mc.getHeight() / scale
    )
}

fun calculateOffsets(screenSize: Pair<Float, Float>, windowSize: Int): List<Float> {
    val width = 9f * 18f
    val height = windowSize / 9f * 18f
    val offsetX = screenSize.first / 2f - width / 2f
    val offsetY = screenSize.second / 2f - height / 2f
    return listOf(offsetX, offsetY, width, height)
}

fun calculateSlotPosition(x: Float, y: Float, offsetX: Float, offsetY: Float): Pair<Int, Int> {
    return Pair(
        floor((x - offsetX) / 18).toInt(),
        floor((y - offsetY) / 18).toInt()
    )
}

fun isBackgroundGlass(item: ItemStack): Boolean = item.getItemId() == 160 && item.metadata == 15

fun renderBackground(offsetX: Number, offsetY: Number, width: Number, height: Number, darkMode: Color) {
    val x = offsetX.toFloat() - 3
    val y = offsetY.toFloat() - 3
    val w = width.toFloat() + 6
    val h = height.toFloat() + 6

    drawRoundedRect(darkMode, x, y, w, h)
    drawRainbowRoundedBorder(x, y, w, h, thickness = 4f)
}

fun renderHeads(container: List<Slot>, windowSize: Int, offsetX: Float, offsetY: Float, slotPosition: Pair<Int, Int>, expand: Int) {
    container.forEach { slot ->
        val i = slot.slotNumber
        val stack = slot.stack
        if (i >= windowSize || stack == null) return@forEach

        val currentOffsetX = i % 9 * 18 + offsetX
        val currentOffsetY = (floor(i / 9.0).toInt() + expand) * 18 + offsetY

        if (isValidSlot(slotPosition.first, slotPosition.second - expand) &&
            getSlotIndex(slotPosition.first, slotPosition.second - expand) == i
        ) drawRoundedRect(
            Color(0, 114, 255),
            currentOffsetX + 0.5f,
            (currentOffsetY) + 0.5f,
            15f, 15f, 1.5f
        )

        if (stack.item is ItemSkull) {
            drawPlayerHead(
                getHeadSkinTexture(stack) ?: return@forEach,
                currentOffsetX + 1.6f,
                currentOffsetY + 1.6f,
                12.8f, 12.8f, 1.5f
            )
        }
    }
}

fun renderItems(container: List<Slot>, windowSize: Int, offsetX: Float, offsetY: Float, expand: Int) {
    container.forEach { slot ->
        val i = slot.slotNumber
        val stack = slot.stack
        if (i >= windowSize) return@forEach
        if (stack == null) return@forEach
        if (stack.item is ItemSkull) return@forEach
        if (isBackgroundGlass(stack)) return@forEach

        renderItem(
            stack,
            i % 9f * 18f + offsetX,
            (floor(i / 9.0).toInt() + expand) * 18f + offsetY
        )
    }
}

fun drawLore(name: String, lore: Collection<String>, mx: Float, my: Float, scale: Float, screenSize: Pair<Float, Float>) {
    drawHoveringText(
        if (name.isBlank()) lore.toMutableList() else lore.toMutableList().apply { add(0, name) },
        (mx * scale).toInt(),
        (my * scale).toInt(),
        (screenSize.first * scale).toInt(),
        (screenSize.second * scale).toInt(),
        - 1, mc.fontRendererObj
    )
}