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
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.send
import kotlin.math.floor


object Rubix {
    private var inTerminal = false
    private var cwid = - 1
    private var windowSize = 0
    private var terminalSlots = mutableListOf<TerminalSlot?>()
    private var clicked = false
    private var queue = mutableListOf<Pair<Int, Int>>()
    private var solution = mutableMapOf<Int, Int>()
    private val allowedSlots = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)
    private val order = listOf(14, 1, 4, 13, 11)


    @SubscribeEvent
    fun onClick(event: GuiMouseClickEvent) {
        if (! inTerminal) return
        event.isCanceled = true

        val termScale = getTermScale()
        val x = getMouseX() / termScale
        val y = getMouseY() / termScale

        val screenWidth = mc.getWidth() / termScale
        val screenHeight = mc.getHeight() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val offsetX = screenWidth / 2 - width / 2
        val offsetY = screenHeight / 2 - height / 2

        val slotX = floor((x - offsetX) / 18).toInt()
        val slotY = floor((y - offsetY) / 18).toInt()

        if (slotX < 0 || slotX > 8 || slotY < 0) return

        val slot = slotX + slotY * 9

        if (slot >= windowSize) return
        val color = solution[slot] ?: return
        val clickType = if (color > 0) 0 else 1
        predict(slot, clickType)

        if (clicked && getClickMode() == ClickMode.QUEUE) {
            queue.add(slot to clickType)
        }
        else click(slot, clickType)

    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inTerminal) return
        event.isCanceled = true

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
        GlStateManager.scale(termScale, termScale, 0f)

        renderBackground(offsetX, offsetY, width, height, colorMode)
        drawText(TerminalSolver.rubixTitle, offsetX, offsetY)

        for (i in 0 until windowSize) {
            val solutionValue = solution[i] ?: continue

            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor(i / 9.0).toInt() * 18 + offsetY

            drawRoundedRect(solverColor, currentOffsetX, currentOffsetY, 16f, 16f, 1.5f)

            drawText(
                solutionValue.toString(),
                currentOffsetX + 8 - getStringWidth(solutionValue.toString()) / 2,
                currentOffsetY + 4,
            )
        }

        GlStateManager.popMatrix()
    }

    private fun solve() {
        solution.clear()
        val calcIndex = { index: Int -> (index + order.size) % order.size }
        val clicks = MutableList(5) { 0 }

        for (i in 0 until 5) {
            terminalSlots.filter { it != null && allowedSlots.contains(it.num) && it.meta != order[calcIndex(i)] }
                .forEach {
                    when (it !!.meta) {
                        order[calcIndex(i - 2)] -> clicks[i] += 2
                        order[calcIndex(i - 1)] -> clicks[i] += 1
                        order[calcIndex(i + 1)] -> clicks[i] += 1
                        order[calcIndex(i + 2)] -> clicks[i] += 2
                    }
                }
        }

        val origin = clicks.indexOf(clicks.minOrNull() ?: 0)
        terminalSlots.filter { it != null && allowedSlots.contains(it.num) && it.meta != order[calcIndex(origin)] }.forEach {
            solution[it !!.num] = when (it.meta) {
                order[calcIndex(origin - 2)] -> 2
                order[calcIndex(origin - 1)] -> 1
                order[calcIndex(origin + 1)] -> - 1
                order[calcIndex(origin + 2)] -> - 2
                else -> 0
            }
        }
    }

    private fun predict(slot: Int, button: Int) {
        val currentSolution = solution[slot] ?: return
        solution[slot] = if (button == 0) currentSolution - 1 else currentSolution + 1
        if (solution[slot] == 0) solution.remove(slot)
    }

    private fun click(slot: Int, button: Int) {
        clicked = true
        C0EPacketClickWindow(cwid, slot, button, if (button == 2) 3 else 0, null, 0).send()
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
        if (! TerminalSolver.rubix.value || LocationUtils.dungeonFloorNumber != 7 || F7Phase != 3) return
        if (event.packet !is S2DPacketOpenWindow) return

        val windowTitle = event.packet.windowTitle.noFormatText
        val slotCount = event.packet.slotCount
        cwid = event.packet.windowId

        if (Regex("^Change all to same color!$").matches(windowTitle)) {
            inTerminal = true
            clicked = false
            terminalSlots.clear()
            windowSize = slotCount
            disableNEUInventoryButtons()
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
        else terminalSlots.add(null)

        if (terminalSlots.size == windowSize && slot == windowSize - 1) {
            solve()
            if (queue.isNotEmpty() && queue.all { (slot, button) ->
                    ((solution[slot] ?: 0) > 0 && button == 0) || ((solution[slot] ?: 0) < 0 && button == 1)
                }) {
                queue.forEach { (slot, button) -> predict(slot, button) }
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
