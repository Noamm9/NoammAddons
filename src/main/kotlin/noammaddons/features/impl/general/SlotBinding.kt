package noammaddons.features.impl.general

import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.PogObject
import noammaddons.events.GuiKeybourdInputEvent
import noammaddons.events.GuiMouseClickEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.GuiUtils.getSlotRenderPos
import noammaddons.utils.RenderUtils.drawLine
import noammaddons.utils.RenderUtils.drawRectBorder
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.favoriteColor
import org.lwjgl.input.Keyboard.*
import java.awt.Color


object SlotBinding: Feature(desc = "Allows you to bind slots to hotbar slots for quick item swaps.") {
    private val showBoundSlots = ToggleSetting("Show Bound Slots", true)
    private val neuStyle = ToggleSetting("Hover Only", false).addDependency { ! showBoundSlots.value }
    private val drawLines = ToggleSetting("Draw Lines", true).addDependency { ! showBoundSlots.value }
    private val drawBorders = ToggleSetting("Draw Borders", true).addDependency { ! showBoundSlots.value }
    private val lineColor = ColorSetting("Line Color", Color.WHITE, false).addDependency { ! showBoundSlots.value || ! drawLines.value }
    private val borderColor = ColorSetting("Border Color", favoriteColor, false).addDependency { ! showBoundSlots.value || ! drawBorders.value }
    private val bindKey = KeybindSetting("Add/Remove Bind Key")

    override fun init() = addSettings(
        SeperatorSetting("Keybind"),
        bindKey,
        SeperatorSetting("Rendering"),
        showBoundSlots, neuStyle,
        drawLines, drawBorders,
        SeperatorSetting("Colors").addDependency { ! showBoundSlots.value },
        lineColor, borderColor
    )

    private val slotBindingData = PogObject("SlotBinds", mutableMapOf<String, Number>())

    private var previousSlot: Int? = null
    private const val PREFIX = "&bSlotBinding &f>"

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onGuiMouseClick(event: GuiMouseClickEvent) {
        val gui = event.gui as? GuiInventory ?: return
        if (! isKeyDown(KEY_LSHIFT) || event.button != 0) return

        val clickedSlotNumber = gui.slotUnderMouse?.slotNumber?.takeIf { it in 5 .. 44 } ?: return

        val inventorySlot: Int
        val hotbarSlot: Int
        val data = slotBindingData.getData()

        if (data.containsKey("$clickedSlotNumber")) {
            inventorySlot = clickedSlotNumber
            hotbarSlot = data["$clickedSlotNumber"]?.toInt() ?: return
        }
        else {
            val entry = data.entries.firstOrNull { it.value == clickedSlotNumber } ?: return
            inventorySlot = entry.key.toInt()
            hotbarSlot = entry.value.toInt()
        }

        val (fromSlot, toHotbarActual) = when {
            inventorySlot in 36 .. 44 && hotbarSlot !in 36 .. 44 -> hotbarSlot to inventorySlot
            inventorySlot !in 36 .. 44 && hotbarSlot in 36 .. 44 -> inventorySlot to hotbarSlot
            else -> {
                if (clickedSlotNumber in 36 .. 44) inventorySlot to clickedSlotNumber
                else hotbarSlot to inventorySlot
            }
        }

        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, fromSlot, toHotbarActual % 36, 2, mc.thePlayer)
        // SoundUtils.click()
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onGuiKeyPress(event: GuiKeybourdInputEvent) {
        val gui = event.gui as? GuiInventory ?: return
        if (event.keyCode != bindKey.value || bindKey.value == KEY_NONE) return
        val clickedSlotNumber = gui.slotUnderMouse?.slotNumber?.takeIf { it in 5 .. 44 } ?: return

        event.isCanceled = true
        //SoundUtils.click()

        val currentPrevious = previousSlot
        if (currentPrevious != null) {
            previousSlot = null

            if (currentPrevious == clickedSlotNumber) return modMessage("$PREFIX &cCannot bind a slot to itself.")

            val firstIsHotbar = currentPrevious in 36 .. 44
            val secondIsHotbar = clickedSlotNumber in 36 .. 44

            if (firstIsHotbar == secondIsHotbar) return modMessage("$PREFIX &cOne slot must be in your inventory and the other in your hotbar.")

            val inventorySlot = if (firstIsHotbar) clickedSlotNumber else currentPrevious
            val hotbarSlot = if (firstIsHotbar) currentPrevious else clickedSlotNumber

            slotBindingData.getData()["$inventorySlot"] = hotbarSlot
            slotBindingData.save()
            modMessage("$PREFIX &aSaved binding&r: Inv Slot &6$inventorySlot &b➜ &rHotbar Slot &6$hotbarSlot")

        }
        else {
            val data = slotBindingData.getData()
            var removed = false

            if (data.containsKey("$clickedSlotNumber")) {
                val boundValue = data.remove("$clickedSlotNumber")
                modMessage("$PREFIX &aRemoved bind&r: Inv Slot &b$clickedSlotNumber &b➜ &rHotbar Slot &b$boundValue")
                removed = true
            }
            else {
                val entryToRemove = data.entries.firstOrNull { it.value == clickedSlotNumber }
                if (entryToRemove != null) {
                    data.remove(entryToRemove.key)
                    modMessage("$PREFIX &aRemoved bind&r: Inv Slot &b${entryToRemove.key} &b➜ &rHotbar Slot &b${entryToRemove.value}")
                    removed = true
                }
            }

            if (removed) slotBindingData.save()
            else previousSlot = clickedSlotNumber
        }
    }

    @SubscribeEvent
    fun onGuiDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        val gui = event.gui as? GuiInventory ?: return
        val data = slotBindingData.getData()
        val hoveredSlotNumber = gui.slotUnderMouse?.slotNumber?.takeIf { it in 5 .. 44 }

        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, 0f, 300f)

        if (showBoundSlots.value) data.takeUnless { it.isEmpty() }?.forEach { (invSlotNum, hbSlotNum) ->
            if (neuStyle.value && ! hoveredSlotNumber.equalsOneOf(invSlotNum.toInt(), hbSlotNum.toInt())) return@forEach
            val invSlotPos = getSlotRenderPos(invSlotNum.toInt())
            val hbSlotPos = getSlotRenderPos(hbSlotNum.toInt())

            if (invSlotPos != null && hbSlotPos != null) {
                if (drawLines.value) drawLine(
                    lineColor.value,
                    invSlotPos.first + 8f, invSlotPos.second + 8f,
                    hbSlotPos.first + 8f, hbSlotPos.second + 8f,
                    2f
                )

                if (drawBorders.value) {
                    drawRectBorder(borderColor.value, invSlotPos.first, invSlotPos.second, 16, 16, 1)
                    drawRectBorder(borderColor.value, hbSlotPos.first, hbSlotPos.second, 16, 16, 1)
                }
            }
        }

        val currentPrevSlot = previousSlot
        if (currentPrevSlot != null && hoveredSlotNumber != null) {
            val startPos = getSlotRenderPos(currentPrevSlot)
            if (startPos != null) {
                drawLine(
                    lineColor.value,
                    startPos.first + 8f, startPos.second + 8f,
                    event.mouseX.toFloat(), event.mouseY.toFloat(),
                    2f
                )
            }
        }
        else if (showBoundSlots.value && drawLines.value && isKeyDown(KEY_LSHIFT) && hoveredSlotNumber != null) {
            val boundToSlotNumber = data["$hoveredSlotNumber"]?.toInt()
                ?: data.entries.find { it.value == hoveredSlotNumber }?.key?.toInt()

            if (boundToSlotNumber != null) {
                val hoveredSlotPos = getSlotRenderPos(hoveredSlotNumber)
                val boundToSlotPos = getSlotRenderPos(boundToSlotNumber)

                if (hoveredSlotPos != null && boundToSlotPos != null) {
                    drawLine(
                        lineColor.value,
                        hoveredSlotPos.first + 8f, hoveredSlotPos.second + 8f,
                        boundToSlotPos.first + 8f, boundToSlotPos.second + 8f,
                        2f
                    )
                }
            }
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onGuiClose(event: GuiOpenEvent) {
        if (event.gui == null || event.gui !is GuiInventory) {
            previousSlot = null
        }
    }
}