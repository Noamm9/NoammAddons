package noammaddons.features.general

import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.KeyBinds.SlotBindingAddBinding
import noammaddons.config.KeyBinds.SlotBindingRemoveBinding
import noammaddons.config.PogObject
import noammaddons.events.GuiMouseClickEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.GuiUtils.getSlotRenderPos
import noammaddons.utils.RenderUtils.drawLine
import noammaddons.utils.RenderUtils.drawRectBorder
import noammaddons.utils.SoundUtils
import org.lwjgl.input.Keyboard.*


object SlotBinding: Feature() {
    private const val PREFIX = "&bSlotBinding &f>"
    private val slotBindingData = PogObject("SlotBinding", mutableMapOf<String, Double?>())

    private val data get() = slotBindingData.getData()
    private var previousSlot: Int? = null

    @SubscribeEvent
    fun onGuiMouseClick(event: GuiMouseClickEvent) {
        if (! config.SlotBinding) return
        val gui = event.gui as? GuiInventory ?: return
        val slot = gui.slotUnderMouse?.slotNumber ?: return
        if (slot < 5 || slot > 44) return
        if (previousSlot != null && isInvalidSlotCombination(slot)) return cancel(event)

        when {
            isKeyDown(KEY_LSHIFT) -> handleShiftKey(slot, event, gui.inventorySlots.windowId)
            isKeyDown(SlotBindingAddBinding.keyCode) -> handleAddBinding(slot, event)
            isKeyDown(SlotBindingRemoveBinding.keyCode) -> handleRemoveBinding(slot, event)
        }
    }

    @SubscribeEvent
    fun renderBinding(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (! config.SlotBinding) return
        if (! config.SlotBindingShowBinding) return
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

    private fun handleShiftClick(slotClicked: Int, windowId: Int) {
        var slot = slotClicked
        var nextSlot = data[slot.toString()]?.rem(36)?.toInt() ?: return

        if (slotClicked > 35) {
            slot = data[slotClicked.toString()]?.rem(36)?.toInt() ?: return
            nextSlot = data.entries.firstOrNull { it.value?.toInt() == slot }?.key?.toInt()?.rem(36) ?: return
        }

        if (nextSlot > 8) return
        mc.playerController.windowClick(windowId, slot, nextSlot, 2, mc.thePlayer)
    }

    private fun cancel(event: GuiMouseClickEvent) {
        modMessage("$PREFIX &cPlease click a valid hotbar slot!")
        event.isCanceled = true
        previousSlot = null
    }

    private fun handleShiftKey(slot: Int, event: GuiMouseClickEvent, windowId: Int) {
        data[slot.toString()]?.let {
            handleShiftClick(slot, windowId)
            SoundUtils.click()
            event.isCanceled = true
        } ?: data.entries.firstOrNull { it.value?.toInt() == slot }?.key?.toInt()?.let {
            handleShiftClick(it, windowId)
            SoundUtils.click()
            event.isCanceled = true
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
        if (! data.removeBinding(slot)) return
        event.isCanceled = true
        modMessage("$PREFIX &aBinding with slot &b$slot &adeleted")
        previousSlot = null
    }

    private fun MutableMap<String, Double?>.removeBinding(slot: Int): Boolean {
        if (remove(slot.toString()) != null) {
            slotBindingData.save()
            return true
        }

        data.entries.firstOrNull { it.value?.toInt() == slot }?.key?.let {
            remove(it)
            slotBindingData.save()
            return true
        }

        return false
    }

    private fun drawBinding(slotPos: Pair<Float, Float>, hotbarSlotPos: Pair<Float, Float>) {
        drawLine(config.SlotBindingLineColor, slotPos.first + 8f, slotPos.second + 8f, hotbarSlotPos.first + 8f, hotbarSlotPos.second + 8f, 3)
        drawRectBorder(config.SlotBindingBorderColor, slotPos.first, slotPos.second, 16, 16, 2)
        drawRectBorder(config.SlotBindingBorderColor, hotbarSlotPos.first, hotbarSlotPos.second, 16, 16, 2)
    }

    private fun isHotbarSlot(slot: Int): Boolean = slot in 36 .. 44
    private fun isInvalidSlotCombination(slot: Int) = isHotbarSlot(previousSlot !!) == isHotbarSlot(slot)
}
