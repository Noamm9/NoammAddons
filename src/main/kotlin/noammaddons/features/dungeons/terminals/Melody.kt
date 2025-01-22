@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package noammaddons.features.dungeons.terminals

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiCloseEvent
import noammaddons.events.GuiMouseClickEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.features.dungeons.terminals.ConstantsVariables.MelodyTitle
import noammaddons.features.dungeons.terminals.ConstantsVariables.TerminalSlot
import noammaddons.features.dungeons.terminals.ConstantsVariables.getColorMode
import noammaddons.features.dungeons.terminals.ConstantsVariables.getSolutionColor
import noammaddons.features.dungeons.terminals.ConstantsVariables.getTermScale
import noammaddons.features.gui.Menus.renderBackground
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.disableNEUInventoryButtons
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import kotlin.math.floor

object Melody: Feature() {
    private var cwid = - 1
    private val terminalSlots = mutableListOf<TerminalSlot?>()
    private var windowSize = 0
    private var isInTerminal = false
    private var correct: Int = - 1
    private var button: Int = - 1
    private var current: Int = - 1


    @SubscribeEvent
    fun onGuiClick(event: GuiMouseClickEvent) {
        if (! isInTerminal) return
        event.isCanceled = true

        val termScale = getTermScale()
        val x = mc.getMouseX() / termScale
        val y = mc.getMouseY() / termScale

        val screenWidth = mc.getWidth().toDouble() / termScale
        val screenHeight = mc.getHeight().toDouble() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val offsetX = screenWidth / 2 - width / 2
        val offsetY = screenHeight / 2 - height / 2

        val slotX = floor((x - offsetX) / 18).toInt()
        val slotY = floor((y - offsetY) / 18).toInt()

        if (slotX < 0 || slotX > 8 || slotY < 0) return

        val slot = slotX + slotY * 9

        if (slot >= windowSize) return

        if (slot.equalsOneOf(16, 25, 34, 43)) {
            sendWindowClickPacket(slot, 0, 0)
            if (config.DevMode) modMessage("clicked slot $slot")
        }
    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! isInTerminal) return
        if (! config.DevMode) event.isCanceled = true

        val termScale = getTermScale()
        val screenWidth = mc.getWidth() / termScale
        val screenHeight = mc.getHeight() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val offsetX = screenWidth / 2 - width / 2
        val offsetY = screenHeight / 2 - height / 2

        val colorMode = getColorMode()
        val solverColor = getSolutionColor()

        GlStateManager.pushMatrix()
        GlStateManager.scale(termScale, termScale, termScale)

        renderBackground(offsetX, offsetY, width, height, colorMode)
        drawText(MelodyTitle, offsetX, offsetY)

        drawRoundedRect(Color(255, 0, 255), offsetX + (correct + 1) * 18, offsetY + 18, 16f, 70f, 1.5f)

        for (i in 0 until windowSize) {
            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor((i / 9f)) * 18f + offsetY

            val buttonSlot = button * 9 + 16
            val currentSlot = button * 9 + 10 + current

            when {
                i == buttonSlot -> drawRoundedRect(solverColor, currentOffsetX, currentOffsetY, 16f, 16f, 1.5f)
                intArrayOf(16, 25, 34, 43).contains(i) -> drawRoundedRect(Color.RED, currentOffsetX, currentOffsetY, 16f, 16f, 1.5f)
                i == currentSlot -> drawRoundedRect(Color(255, 116, 0), currentOffsetX, currentOffsetY, 16f, 16f, 1.5f)
            }
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onS2FPacketSetSlot(event: PacketEvent.Received) {
        if (! isInTerminal) return
        if (event.packet !is S2FPacketSetSlot) return

        val itemStack = event.packet.func_149174_e()
        val slot = event.packet.func_149173_d()

        if (slot < 0) return
        if (slot >= windowSize) return

        if (itemStack !== null) {
            terminalSlots[slot] = TerminalSlot(
                slot,
                itemStack.getItemId(),
                itemStack.metadata,
                itemStack.stackSize,
                itemStack.displayName.removeFormatting(),
                itemStack.isItemEnchanted,
            )


            if (terminalSlots[slot] != null) {
                if (terminalSlots[slot] !!.id === 160 && terminalSlots[slot] !!.meta === 5) {
                    val correct1 = terminalSlots.find { (it?.id ?: 0) == 160 && (it?.meta ?: 0) === 2 }?.num?.minus(1)
                    val button1 = floor((slot / 9).toDouble()) - 1
                    val current1 = slot % 9 - 1
                    if (correct1 != null) correct = correct1
                    button = button1.toInt()
                    current = current1
                }
            }
        }
        else terminalSlots[slot] = null
    }

    @SubscribeEvent
    fun onWindowOpen(event: PacketEvent.Received) {
        if (! config.CustomTerminalsGui || ! config.CustomMelodyTerminal || dungeonFloor != 7 || F7Phase != 3) return
        if (event.packet !is S2DPacketOpenWindow) return

        val windowTitle = event.packet.windowTitle.noFormatText
        val slotCount = event.packet.slotCount
        cwid = event.packet.windowId

        if (! windowTitle.matches(Regex("Click the button on time!"))) return

        terminalSlots.clear()
        repeat(slotCount) { terminalSlots.add(null) }
        disableNEUInventoryButtons()
        windowSize = slotCount
        isInTerminal = true
    }

    @SubscribeEvent
    fun onWindowClose(event: GuiCloseEvent) {
        if (! isInTerminal) return
        if (event.newGui != null) return
        isInTerminal = false
    }
}
