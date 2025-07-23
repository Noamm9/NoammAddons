package noammaddons.features.impl.dungeons.solvers.terminals.impl

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver
import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver.getClickMode
import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver.getColorMode
import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver.getSolutionColor
import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver.getTermScale
import noammaddons.features.impl.dungeons.solvers.terminals.core.ClickMode
import noammaddons.features.impl.dungeons.solvers.terminals.core.TerminalSlot
import noammaddons.features.impl.gui.Menus.renderBackground
import noammaddons.NoammAddons.Companion.mc
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.disableNEUInventoryButtons
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.LocationUtils
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.MouseUtils.getMouseX
import noammaddons.utils.MouseUtils.getMouseY
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.send
import kotlin.math.floor


object Numbers {
    private var inTerminal = false
    private var cwid = - 1
    private var windowSize = 0
    private val terminalSlots = mutableListOf<TerminalSlot>()
    private var clicked = false
    private val queue = mutableListOf<Pair<Int, Int>>()
    private val solution = mutableListOf<Int>()
    private val allowedSlots = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25
    )

    @SubscribeEvent
    fun onClick(event: GuiMouseClickEvent) {
        if (! inTerminal) return
        event.isCanceled = true

        val termScale = getTermScale()
        val x = getMouseX() / termScale
        val y = getMouseY() / termScale

        val screenWidth = mc.getWidth().toDouble() / termScale
        val screenHeight = mc.getHeight().toDouble() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val offsetX = screenWidth / 2f - width / 2f
        val offsetY = screenHeight / 2f - height / 2f

        val slotX = floor((x - offsetX) / 18).toInt()
        val slotY = floor((y - offsetY) / 18).toInt()

        if (slotX < 0 || slotX > 8 || slotY < 0) return

        val slot = slotX + slotY * 9

        if (slot >= windowSize) return
        if (solution.indexOf(slot) != 0) return
        predict(slot)

        if (clicked && getClickMode() == ClickMode.QUEUE) {
            queue.add(slot to 0)
        }
        else click(slot, 0)

    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inTerminal) return
        disableNEUInventoryButtons()
        event.isCanceled = true

        val termScale = getTermScale()
        val screenWidth = mc.getWidth() / termScale
        val screenHeight = mc.getHeight() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val offsetX = screenWidth / 2 - width / 2
        val offsetY = screenHeight / 2 - height / 2

        val colorMode = getColorMode()
        var solverColor = getSolutionColor()

        GlStateManager.pushMatrix()
        GlStateManager.scale(termScale, termScale, termScale)

        renderBackground(offsetX, offsetY, width, height, colorMode)
        drawText(TerminalSolver.numbersTitle, offsetX, offsetY)

        for (i in 0 until windowSize) {
            val index = solution.indexOf(i)
            if (index == - 1 || index >= 3) continue

            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor(i / 9.0).toInt() * 18 + offsetY

            repeat(index) { solverColor = solverColor.darker().darker() }

            drawRoundedRect(solverColor, currentOffsetX, currentOffsetY, 16f, 16f, 1.5f)

            repeat(index) { solverColor = solverColor.brighter().brighter() }

            val stackSize = mc.thePlayer?.openContainer?.getSlot(i)?.stack?.stackSize ?: continue
            drawCenteredText("$stackSize", currentOffsetX + 8, currentOffsetY + 4)
        }
        GlStateManager.popMatrix()
    }

    private fun solve() {
        solution.clear()
        terminalSlots.filter { allowedSlots.contains(it.num) && it.id == 160 && it.meta == 14 }
            .sortedBy { it.size }
            .map { it.num }
            .forEach { solution.add(it) }
    }

    private fun predict(slot: Int) {
        if (solution.indexOf(slot) != 0) return
        solution.removeAt(0)
    }

    private fun click(slot: Int, button: Int) {
        clicked = true
        C0EPacketClickWindow(cwid, slot, button, 0, null, 0).send()
        val initialWindowId = cwid
        setTimeout(TerminalSolver.reSyncTime) {
            if (! inTerminal || initialWindowId != cwid) return@setTimeout
            queue.clear()
            solve()
            clicked = false
        }
    }

    @SubscribeEvent
    fun onWindowOpen(event: PacketEvent.Received) {
        if (! TerminalSolver.numbers.value || LocationUtils.dungeonFloorNumber != 7 || F7Phase != 3) return
        if (event.packet !is S2DPacketOpenWindow) return

        val windowTitle = event.packet.windowTitle.noFormatText
        val slotCount = event.packet.slotCount
        cwid = event.packet.windowId

        if (windowTitle.matches(Regex("^Click in order!$"))) {
            inTerminal = true
            clicked = false
            terminalSlots.clear()
            windowSize = slotCount
        }
        else inTerminal = false
    }

    @SubscribeEvent
    fun onS2FPacketSetSlot(event: PacketEvent.Received) {
        if (! inTerminal) return
        if (event.packet !is S2FPacketSetSlot) return

        val itemStack = event.packet.func_149174_e()
        val slot = event.packet.func_149173_d()

        if (slot < 0) return
        if (slot >= windowSize) return

        if (itemStack !== null) {
            terminalSlots.add(
                TerminalSlot(
                    slot,
                    itemStack.getItemId(),
                    itemStack.metadata,
                    itemStack.stackSize,
                    itemStack.displayName.removeFormatting(),
                    itemStack.isItemEnchanted,
                )
            )
        }

        if (terminalSlots.size == windowSize) {
            solve()
            if (queue.isNotEmpty() && queue.all { (queuedSlot, _) ->
                    solution.indexOf(queuedSlot) == queue.indexOfFirst { it.first == queuedSlot }
                }) {
                queue.forEach { (queuedSlot, _) -> predict(queuedSlot) }
                click(queue[0].first, queue[0].second)
                queue.removeAt(0)
            }
            else queue.clear()

        }
    }

    @SubscribeEvent
    fun onWindowClose(event: GuiCloseEvent) {
        if (! inTerminal) return
        if (event.newGui != null) return
        inTerminal = false
        queue.clear()
    }
}
