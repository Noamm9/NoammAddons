package noammaddons.features.gui.Menus

import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText
import noammaddons.features.gui.Menus.CustomMenuManager.menuList
import noammaddons.features.gui.ScalableTooltips
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.disableNEUInventoryButtons
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.ItemUtils.getHeadSkinTexture
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawPlayerHead
import noammaddons.utils.RenderUtils.drawRainbowRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils.renderItem
import noammaddons.utils.SoundUtils
import noammaddons.utils.Utils.isNull
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
val backgroundColor = Color(33, 33, 33, 150)


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

    SoundUtils.click.start()
}

fun inMenu(): Boolean {
    if (! config.CustomMenus) return false
    if (mc.currentScreen !is GuiChest) return false

    val isWhitelisted = menuList.any { (name, regex, cfg) ->
        val nameCheck = name?.let { currentChestName.contains(it) } ?: false
        val regexCheck = regex?.matches(currentChestName) == true
        val configCheck = cfg.invoke()

        (nameCheck || regexCheck) && configCheck
    }

    if (isWhitelisted) disableNEUInventoryButtons()
    return isWhitelisted
}


fun calculateScale(): Float {
    return (7f * config.CustomSBMenusScale) / mc.getScaleFactor()
}

fun shouldExpand(container: List<Slot>): Int {
    if (mc.currentScreen !is GuiContainer) return 0

    for (slot in container) {
        val stack = slot.stack
        val i = slot.slotNumber

        if (i < 7) {
            if (! stack.isNull()) {
                if (! isBackgroundGlass(stack)) {
                    return 1
                }
            }
        }
    }
    return 0
}

fun getMouseScaledCoordinates(scale: Float): Pair<Float, Float> {
    return Pair(
        mc.getMouseX() / scale,
        mc.getMouseY() / scale
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
    drawRainbowRoundedBorder(x, y, w, h, thickness = 3f)
}

fun renderMenuTitle(name: String?, offsetX: Float, offsetY: Float) {
    if (! name.isNullOrEmpty()) {
        drawText(
            "&6&l&n[&b&l&nN&d&l&nA&6&l&n]&r &b&l$name".addColor(),
            offsetX, offsetY, 1f, Color.WHITE
        )
    }
}

fun renderHeads(container: List<Slot>, windowSize: Int, offsetX: Float, offsetY: Float, slotPosition: Pair<Int, Int>, expand: Int) {
    container.forEach { slot ->
        val i = slot.slotNumber
        val stack = slot.stack
        if (i >= windowSize || stack.isNull()) return@forEach

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

        if (stack.item is ItemSkull) drawPlayerHead(
            getHeadSkinTexture(stack) ?: return@forEach,
            currentOffsetX + 1.6f,
            currentOffsetY + 1.6f,
            12.8f, 12.8f, 1.5f
        )
    }
}

fun renderItems(container: List<Slot>, windowSize: Int, offsetX: Float, offsetY: Float, expand: Int) {
    container.forEach { slot ->
        val i = slot.slotNumber
        val stack = slot.stack
        if (i >= windowSize) return@forEach
        if (stack.isNull()) return@forEach
        if (stack.item is ItemSkull) return@forEach
        if (isBackgroundGlass(stack)) return@forEach

        renderItem(
            stack,
            i % 9f * 18f + offsetX,
            (floor(i / 9.0).toInt() + expand) * 18f + offsetY
        )
    }
}

fun drawLore(name: String, lore: List<String>, mx: Float, my: Float, scale: Float, screenSize: Pair<Float, Float>) {
    drawHoveringText(
        lore.toMutableList().apply { add(0, name) },
        (mx * scale).toInt(),
        (my * scale).toInt(),
        (screenSize.first * scale).toInt(),
        (screenSize.second * scale).toInt(),
        - 1, mc.fontRendererObj
    )
}