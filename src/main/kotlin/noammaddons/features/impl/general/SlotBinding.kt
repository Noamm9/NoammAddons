package noammaddons.features.impl.general

import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.PogObject
import noammaddons.events.GuiMouseClickEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.GuiUtils.getSlotRenderPos
import noammaddons.utils.RenderUtils.drawLine
import noammaddons.utils.RenderUtils.drawRectBorder
import noammaddons.utils.SoundUtils
import org.lwjgl.input.Keyboard.*
import java.awt.Color


object SlotBinding: Feature("Allows you to bind slots to hotbar slots for quick item swaps by shift clicking the slot") {
    private val showBoundSlots = ToggleSetting("Show Bound Slots", true)
    private val drawLines = ToggleSetting("Draw Lines", true)
    private val drawBorders = ToggleSetting("Draw Borders", true)

    private val addBindKey = KeybindSetting("Add Bind Key")
    private val removeBindKey = KeybindSetting("Remove Bind Key")

    private val lineColor = ColorSetting("Line Color", Color.WHITE, false)
    private val borderColor = ColorSetting("Border Color", Color.CYAN, false)

    override fun init() = addSettings(
        showBoundSlots, drawLines, drawBorders,
        SeperatorSetting("Keybinds"),
        addBindKey, removeBindKey,
        SeperatorSetting("Colors"),
        lineColor, borderColor
    )

    private const val PREFIX = "&bSlotBinding &f>"
    private val slotBindingData = PogObject("SlotBinding", mutableMapOf<String, Double?>())

    private val data get() = slotBindingData.getData()
    private var previousSlot: Int? = null

    @SubscribeEvent
    fun onGuiMouseClick(event: GuiMouseClickEvent) {
        val gui = event.gui as? GuiInventory ?: return
        val slot = gui.slotUnderMouse?.slotNumber ?: return
        if (slot < 5 || slot > 44) return
        if (previousSlot != null && isInvalidSlotCombination(slot)) {
            modMessage("$PREFIX &cPlease click a valid hotbar slot!")
            event.isCanceled = true
            previousSlot = null
        }

        when {
            isKeyDown(KEY_LSHIFT) -> handleShiftInteraction(slot, event, gui.inventorySlots.windowId)
            isKeyDown(addBindKey.value) -> handleAddBinding(slot, event)
            isKeyDown(removeBindKey.value) -> handleRemoveBinding(slot, event)
        }
    }

    @SubscribeEvent
    fun renderBinding(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (! showBoundSlots.value) return
        if (event.gui !is GuiInventory) return
        if (data.isEmpty()) return

        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, 0f, 300f)

        data.forEach { (slotStr, hotbarSlotDouble) ->
            val slotIndex = slotStr.toIntOrNull() ?: return@forEach
            val hotbarSlotIndex = hotbarSlotDouble?.toInt() ?: return@forEach

            val slotPos = getSlotRenderPos(slotIndex) ?: return@forEach
            val hotbarSlotPos = getSlotRenderPos(hotbarSlotIndex) ?: return@forEach

            drawBinding(slotPos, hotbarSlotPos)
        }

        GlStateManager.popMatrix()
    }

    private fun handleShiftInteraction(slot: Int, event: GuiMouseClickEvent?, windowId: Int) {
        val validSlot = data[slot.toString()]?.let {
            slot
        } ?: data.entries.firstOrNull { it.value?.toInt() == slot }?.key?.toInt()

        if (validSlot == null) return

        var actualSlot = validSlot
        var nextSlot = data[actualSlot.toString()]?.rem(36)?.toInt() ?: return

        if (validSlot > 35) {
            actualSlot = data[validSlot.toString()]?.rem(36)?.toInt() ?: return
            nextSlot = data.entries.firstOrNull { it.value?.toInt() == actualSlot }?.key?.toInt()?.rem(36) ?: return
        }

        if (nextSlot > 8) return

        mc.playerController.windowClick(windowId, actualSlot, nextSlot, 2, mc.thePlayer)

        event?.let {
            SoundUtils.click()
            it.isCanceled = true
        }
    }

    private fun handleAddBinding(slot: Int, event: GuiMouseClickEvent) {
        previousSlot = previousSlot ?: slot
        SoundUtils.click()

        if (previousSlot != null && previousSlot != slot) {
            data[previousSlot.toString()] = slot.toDouble()
            slotBindingData.save()
            modMessage("$PREFIX &aSaved binding&r: &6${previousSlot} &b➜ &6$slot")
            previousSlot = null
        }

        event.isCanceled = true
    }

    private fun handleRemoveBinding(slot: Int, event: GuiMouseClickEvent) {
        previousSlot = previousSlot ?: slot
        SoundUtils.click()

        val key = slot.toString()
        val removed = if (data.remove(key) != null) true
        else data.entries.firstOrNull { it.value?.toInt() == slot }?.key?.let {
            data.remove(it)
        } != null


        if (! removed) return

        slotBindingData.save()
        event.isCanceled = true
        modMessage("$PREFIX &aBinding with slot &b$slot &adeleted")
        previousSlot = null
    }

    private fun drawBinding(slotPos: Pair<Float, Float>, hotbarSlotPos: Pair<Float, Float>) {
        if (drawLines.value) drawLine(lineColor.value, slotPos.first + 8f, slotPos.second + 8f, hotbarSlotPos.first + 8f, hotbarSlotPos.second + 8f, 3)
        if (! drawBorders.value) return
        drawRectBorder(borderColor.value, slotPos.first, slotPos.second, 16, 16, 2)
        drawRectBorder(borderColor.value, hotbarSlotPos.first, hotbarSlotPos.second, 16, 16, 2)
    }

    private fun isInvalidSlotCombination(slot: Int) = previousSlot in 36 .. 44 == slot in 36 .. 44
}
