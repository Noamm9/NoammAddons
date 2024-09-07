package NoammAddons.features.dungeons.terminals

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.sounds.AYAYA
import NoammAddons.events.GuiContainerEvent
import NoammAddons.events.ReceivePacketEvent
import NoammAddons.events.SentPacketEvent
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.getTermScale
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.Slot
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.RubixTitle
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ItemUtils.getItemId
import NoammAddons.utils.LocationUtils
import NoammAddons.utils.LocationUtils.inBoss
import NoammAddons.utils.RenderUtils
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import NoammAddons.utils.ThreadUtils.setTimeout
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.getColorMode
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.getSolutionColor
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.utils.GuiUtils.disablePatcherScale
import NoammAddons.utils.GuiUtils.enablePatcherScale
import NoammAddons.utils.GuiUtils.getMouseX
import NoammAddons.utils.GuiUtils.getMouseY
import NoammAddons.utils.LocationUtils.F7Phase
import NoammAddons.utils.LocationUtils.P3Section
import net.minecraftforge.client.event.RenderGameOverlayEvent
import kotlin.math.floor


object Rubix {
    private var inTerminal = false
    private var cwid = -1
    private var windowSize = 0
    private var slots = mutableListOf<Slot?>()

    private var clicked = false
    private var queue = mutableListOf<Pair<Int, Int>>()
    private var solution = mutableMapOf<Int, Int>()
    private val allowedSlots = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)
    private val order = listOf(14, 1, 4, 13, 11)

    
    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (!inTerminal) return
        event.isCanceled = true


        val x = mc.getMouseX() / getTermScale()
        val y = mc.getMouseY() / getTermScale()

        val termScale = getTermScale()
        val screenWidth = mc.getWidth().toDouble() / termScale
        val screenHeight = mc.getHeight().toDouble() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val globalOffsetX = 0.0
        val globalOffsetY = 0.0

        val offsetX = (screenWidth / 2 - width / 2 + globalOffsetX)
        val offsetY = (screenHeight / 2 - height / 2 + globalOffsetY)

        val slotX = floor((x - offsetX) / 18).toInt()
        val slotY = floor((y - offsetY) / 18).toInt()

        if (slotX < 0 || slotX > 8 || slotY < 0) return

        val slot = slotX + slotY * 9

        if (slot >= windowSize) return

        when {
            (solution[slot] ?: 0) > 0 -> {
                predict(slot, 0)
                if (clicked) queue.add(slot to 0) else click(slot, 0)
            }
            (solution[slot] ?: 0) < 0 -> {
                predict(slot, 1)
                if (clicked) queue.add(slot to 1) else click(slot, 1)
            }
        }
    }

    @SubscribeEvent
    fun render(event: RenderGameOverlayEvent.Pre) {
        if (RenderGameOverlayEvent.ElementType.HOTBAR != event.type) return
        if (!inTerminal) return

        val termScale = getTermScale()
        val screenWidth = mc.getWidth().toDouble() / termScale
        val screenHeight = mc.getHeight().toDouble() / termScale

        val width = 9 * 18.0
        val height = (windowSize / 9 * 18).toDouble()

        val globalOffsetX = 0.0
        val globalOffsetY = 0.0

        val offsetX = (screenWidth / 2 - width / 2 + globalOffsetX)
        val offsetY = (screenHeight / 2 - height / 2 + globalOffsetY)

        val colorMode = getColorMode()
        val solverColor = getSolutionColor()

        GlStateManager.pushMatrix()
        GlStateManager.scale(termScale, termScale, 0.0)

        RenderUtils.drawRoundedRect(
            colorMode.darker(),
            offsetX - 2 - (width / 15) / 2,
            offsetY - 2 - (width / 15) / 2,
            width + 4 + width / 15,
            height + 4 + width / 15
        )

        RenderUtils.drawRoundedRect(
            colorMode,
            offsetX - 3,
            offsetY - 3,
            width + 6,
            height + 6
        )

        RenderUtils.drawText(RubixTitle, offsetX, offsetY)


        for (i in 0 until windowSize) {
            val solutionValue = solution[i] ?: continue

            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor(i / 9.0).toInt() * 18 + offsetY

            RenderUtils.drawRoundedRect(solverColor, currentOffsetX, currentOffsetY, 16.0, 16.0, .0)

            RenderUtils.drawText(
                solutionValue.toString(),
                (currentOffsetX + 8)  - mc.fontRendererObj.getStringWidth(solutionValue.toString())/2,
                currentOffsetY + 4
            )
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (!inTerminal) return
        if (!config.forceSkyblock) event.isCanceled = true
    }

    private fun solve() {
        solution.clear()
        val calcIndex = { index: Int -> (index + order.size) % order.size }
        val clicks = MutableList(5) { 0 }

        for (i in 0 until 5) {
            slots.filter {it != null && allowedSlots.contains(it.num) && it.meta != order[calcIndex(i)] }
                .forEach {
                    when (it!!.meta) {
                        order[calcIndex(i - 2)] -> clicks[i] += 2
                        order[calcIndex(i - 1)] -> clicks[i] += 1
                        order[calcIndex(i + 1)] -> clicks[i] += 1
                        order[calcIndex(i + 2)] -> clicks[i] += 2
                    }
                }
        }

        val origin = clicks.indexOf(clicks.minOrNull() ?: 0)
        slots.filter{it != null && allowedSlots.contains(it.num) && it.meta != order[calcIndex(origin)]}.forEach{
            solution[it!!.num] = when (it.meta) {
                order[calcIndex(origin - 2)] -> 2
                order[calcIndex(origin - 1)] -> 1
                order[calcIndex(origin + 1)] -> -1
                order[calcIndex(origin + 2)] -> -2
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
        mc.netHandler.addToSendQueue(C0EPacketClickWindow(cwid, slot, button, if (button == 2) 3 else 0, null, 0))
        val initialWindowId = cwid
        setTimeout(1000) {
            if (!inTerminal || initialWindowId != cwid) return@setTimeout
            queue.clear()
            solve()
            clicked = false
        }
    }


    @SubscribeEvent
    fun onWindowOpen(event: ReceivePacketEvent) {
        if (!config.CustomTerminalsGui || !config.CustomRubixTerminal || LocationUtils.dungeonFloor != 7 || F7Phase != 3) return
        if (event.packet !is S2DPacketOpenWindow) return

        val windowTitle = event.packet.windowTitle.unformattedText.removeFormatting()
        val slotCount = event.packet.slotCount
        cwid = event.packet.windowId

        if (Regex("^Change all to same color!$").matches(windowTitle)) {
            inTerminal = true
            disablePatcherScale()
            clicked = false
            slots.clear()
            windowSize = slotCount
        }
        else inTerminal = false
    }

    @SubscribeEvent
    fun onS2FPacketSetSlot(event: ReceivePacketEvent) {
        if (!inTerminal) return
        if (event.packet !is S2FPacketSetSlot) return

        val itemStack = event.packet.func_149174_e()
        val slot = event.packet.func_149173_d()

        if (slot < 0) return
        if (slot >= windowSize) return

        if (itemStack !== null) {
            slots.add(
                Slot(
                    slot,
                    itemStack.getItemId(),
                    itemStack.metadata,
                    itemStack.stackSize,
                    itemStack.displayName.removeFormatting(),
                    itemStack.isItemEnchanted,
                )
            )
        }
        else slots.add(null)

        if (slots.size == windowSize && slot == windowSize - 1) {
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
    fun onWindowClose(event: ReceivePacketEvent) {
        if (event.packet !is S2EPacketCloseWindow) return
        if (!inTerminal) return
        reset()
        AYAYA.play()
    }

    @SubscribeEvent
    fun onSentPacket(event: SentPacketEvent) {
        if (event.packet !is C0DPacketCloseWindow) return
        if (!inTerminal) return
        reset()
    }

    private fun reset() {
        inTerminal = false
        queue.clear()
        enablePatcherScale()
    }
}
