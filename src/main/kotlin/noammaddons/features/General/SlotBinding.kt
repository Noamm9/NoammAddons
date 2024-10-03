package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.config.KeyBinds.SlotBinding
import noammaddons.config.PogObject
import noammaddons.events.GuiContainerEvent
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.RenderUtils
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.RenderUtils.drawLine
import noammaddons.utils.RenderUtils.drawRoundedBorder
import org.lwjgl.input.Keyboard
import java.awt.Color


// heavily based on DocilElm's CT module - SlotBinding
// https://github.com/DocilElm/SlotBinding/blob/main/SlotBinding/index.js
object SlotBinding {
    private val SlotBindingData = PogObject("SlotBinding", mutableMapOf<String, Double?>())
    private val data get() = SlotBindingData.getData()
    private val prefix = "&7&bSlotBinding&7 &7>"
    private var previousSlot: Int? = null

    private fun handleShiftClick(slotClicked: Int) {
        val container = mc.thePlayer.openContainer
        val hotbarSlot = data[slotClicked.toString()]?.rem(36) ?: return
        if (hotbarSlot >= 9) return

        mc.playerController.windowClick(
            container.windowId,
            slotClicked,
            hotbarSlot.toInt(),
            2,
            mc.thePlayer
        )
    }


    @SubscribeEvent
    fun onGuiMouseClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (!config.SlotBinding) return

        val gui = event.guiScreen
        if (gui !is GuiInventory) return

        val slot = gui.slotUnderMouse?.slotNumber ?: return

        if (slot < 5) return

        if (previousSlot != null && (slot < 36 || slot > 44)) {
            modMessage("$prefix &cPlease click a valid hotbar slot!")
            previousSlot = null
            return
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && slot.toString() in data.keys) {
            event.isCanceled = true
            handleShiftClick(slot)
            return
        }
	    
        if (!Keyboard.isKeyDown(SlotBinding.keyCode)) return
        if (previousSlot == null) previousSlot = slot


        if ("$slot" in data.keys) {
            data.remove("$slot")
            SlotBindingData.save()
            modMessage("$prefix &aBinding with slot &b$slot &adeleted")
            return
        }

        if (!data.containsKey(slot.toString()) && previousSlot == null) {
            data[slot.toString()] = null
            SlotBindingData.save()
        }

        event.isCanceled = true

        if (slot == previousSlot) return

        data[(previousSlot ?: return).toString()] = slot.toDouble()
        SlotBindingData.save()
        modMessage("$prefix &aSaved binding&r: &6${previousSlot} &b➜ &6$slot")
        previousSlot = null
    }

    @SubscribeEvent
    fun renderBinding(e: GuiContainerEvent.DrawSlotEvent) {
        if (!config.SlotBinding) return
        if (!config.SlotBindingShowBinding) return
        if (mc.currentScreen !is GuiInventory) return
        if (data.isEmpty()) return
        data.entries.forEach { (slot_, hotbarSlot_) ->
            val slotIndex = slot_.toIntOrNull() ?: return@forEach
            val hotbarSlotIndex = hotbarSlot_?.toInt() ?: return@forEach

            val slot = e.container.getSlot(slotIndex)
            val hotbarSlot = e.container.getSlot(hotbarSlotIndex)

            drawLine(
                Color(0, 255, 255),
                slot.xDisplayPosition + 8f,
                slot.yDisplayPosition + 8f,
                hotbarSlot.xDisplayPosition + 8f,
                hotbarSlot.yDisplayPosition + 8f,
                1f
            )


            drawRoundedBorder(
                Color(0, 255, 255),
                slot.xDisplayPosition * 1f,
                slot.yDisplayPosition * 1f,
                16f, 16f, 0f
            )
        }
    }
}