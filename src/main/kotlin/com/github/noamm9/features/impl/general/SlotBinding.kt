package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.inventory.ClickType
import org.lwjgl.glfw.GLFW
import java.awt.Color

object SlotBinding: Feature("Allows you to bind slots to hotbar slots for quick item swaps.") {
    private val bindKey by KeybindSetting("Binding key", GLFW.GLFW_KEY_R).section("Keybind").withDescription("Hold this key and click a hotbar slot and an inventory slot to link them.")
    private val simpleClick by ToggleSetting("Simple Click Swap", false).withDescription("Swap bound slots with a normal click instead of Shift+Click.")
    private val showBoundSlots by ToggleSetting("Show Bound Slots", true).section("Rendering")
    private val neuStyle by ToggleSetting("Hover Only", false).withDescription("Only shows bound slots when hovering over a them.").showIf { showBoundSlots.value }
    private val drawBorders by ToggleSetting("Draw Border", true).showIf { showBoundSlots.value }
    private val drawLines by ToggleSetting("Draw Line", true).showIf { showBoundSlots.value }
    private val borderColor by ColorSetting("Border Color", Color.PINK, false).showIf { showBoundSlots.value && drawBorders.value }.section("Colors")
    private val lineColor by ColorSetting("Line Color", Color.WHITE, false).showIf { showBoundSlots.value && drawLines.value }

    @Suppress("UNCHECKED_CAST")
    private val binds by lazy {
        (cacheData.getData()["slotbindings"] as? Map<String, Number> ?: mutableMapOf()).toMutableMap()
    }

    private var previousSlot: Int? = null

    override fun init() {
        register<ContainerEvent.MouseClick> {
            val screen = event.screen as? InventoryScreen ?: return@register
            val slotId = (screen as IAbstractContainerScreen).hoveredSlot?.index ?: return@register

            if (bindKey.isDown()) {
                event.isCanceled = true

                val currentPrev = previousSlot
                if (currentPrev != null) {
                    previousSlot = null
                    if (currentPrev == slotId) return@register

                    val firstIsHb = currentPrev in 36 .. 44
                    val secondIsHb = slotId in 36 .. 44

                    if (firstIsHb != secondIsHb) {
                        val inv = if (firstIsHb) slotId else currentPrev
                        val hb = if (firstIsHb) currentPrev else slotId
                        binds[inv.toString()] = hb
                        cacheData.getData()["slotbindings"] = binds
                        cacheData.save()
                        modMessage("Bound $inv to $hb")
                    }
                }
                else {
                    val existingBind = binds[slotId.toString()] ?: binds.entries.find { it.value == slotId }?.key

                    if (existingBind != null) {
                        if (binds.containsKey(slotId.toString())) binds.remove(slotId.toString())
                        else binds.entries.removeIf { it.value == slotId }

                        cacheData.getData()["slotbindings"] = binds
                        cacheData.save()
                        modMessage("Bind removed")
                    }
                    else previousSlot = slotId

                }
                return@register
            }

            if (event.button != 0) return@register
            val isShiftDown = (event.modifiers and GLFW.GLFW_MOD_SHIFT) != 0
            if (! simpleClick.value && ! isShiftDown) return@register

            val boundPartner = binds[slotId.toString()]?.toInt() ?: binds.entries.find { it.value.toInt() == slotId }?.key?.toInt() ?: return@register

            event.isCanceled = true

            val hotbarIndex = if (slotId in 36 .. 44) slotId - 36 else boundPartner - 36
            val inventorySlot = if (slotId in 36 .. 44) boundPartner else slotId

            if (mc.player == null || mc.gameMode == null) return@register
            mc.gameMode !!.handleInventoryMouseClick(mc.player !!.containerMenu.containerId, inventorySlot, hotbarIndex, ClickType.SWAP, mc.player)
        }

        register<ContainerEvent.Close> {
            previousSlot = null
        }
    }

    @JvmStatic
    fun drawSlotBinding(context: GuiGraphics, mouseX: Int, mouseY: Int, screen: AbstractContainerScreen<*>) {
        if (! enabled) return
        if (screen !is InventoryScreen) return
        val hoveredSlot = (screen as IAbstractContainerScreen).hoveredSlot?.index

        if (showBoundSlots.value) {
            binds.forEach { (inv, hb) ->
                if (neuStyle.value && (hoveredSlot != inv.toInt() && hoveredSlot != hb.toInt())) return@forEach

                val p1 = GuiUtils.getSlotPos(screen, inv.toInt()) ?: return@forEach
                val p2 = GuiUtils.getSlotPos(screen, hb.toInt()) ?: return@forEach

                if (drawLines.value) Render2D.drawLine(
                    context,
                    p1.first + 8, p1.second + 8,
                    p2.first + 8, p2.second + 8,
                    lineColor.value
                )

                if (drawBorders.value) {
                    Render2D.drawBorder(context, p1.first.toInt(), p1.second.toInt(), 16, 16, borderColor.value)
                    Render2D.drawBorder(context, p2.first.toInt(), p2.second.toInt(), 16, 16, borderColor.value)
                }
            }
        }

        previousSlot?.let {
            val p = GuiUtils.getSlotPos(screen, it) ?: return@let
            Render2D.drawLine(context, p.first + 8, p.second + 8, mouseX, mouseY, lineColor.value, 1f)
        }
    }
}