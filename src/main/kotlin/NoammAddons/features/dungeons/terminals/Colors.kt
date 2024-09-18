package NoammAddons.features.dungeons.terminals

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.GuiContainerEvent
import NoammAddons.events.PacketEvent
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.ColorsTitle
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.Slot
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.getColorMode
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.getSolutionColor
import NoammAddons.features.dungeons.terminals.ConstantsVeriables.getTermScale
import NoammAddons.sounds.AYAYA
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.GuiUtils.getMouseX
import NoammAddons.utils.GuiUtils.getMouseY
import NoammAddons.utils.ItemUtils.getItemId
import NoammAddons.utils.LocationUtils
import NoammAddons.utils.LocationUtils.F7Phase
import NoammAddons.utils.RenderUtils
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import NoammAddons.utils.ThreadUtils.setTimeout
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.floor

object Colors {
    private var inTerminal = false
    private var cwid = -1
    private var windowSize = 0
    private val slots = mutableListOf<Slot?>()

    private var clicked = false
    private val queue = mutableListOf<Pair<Int, Int>>()
    private val solution = mutableListOf<Int>()
    private var extra: String? = null

    private val allowedSlots = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
    private val replacements = mapOf(
        "light gray" to "silver", "wool" to "white", "bone" to "white", "ink" to "black",
        "lapis" to "blue", "cocoa" to "brown", "dandelion" to "yellow", "rose" to "red", "cactus" to "green"
    )


    @SubscribeEvent
    fun onGuiClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (!inTerminal) return
        event.isCanceled = true

        val termScale = getTermScale()
        val x = mc.getMouseX() / termScale
        val y = mc.getMouseY() / termScale

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

        if (solution.contains(slot)) {
            predict(slot, 0)
            if (clicked) queue.add(slot to 0) else click(slot, 0)
        }
    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (!inTerminal) return
        if (!config.DevMode) event.isCanceled = true

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

        mc.fontRendererObj.drawStringWithShadow(ColorsTitle, offsetX.toFloat(), offsetY.toFloat(), Color.WHITE.rgb)

        for (i in 0 until windowSize) {
            if (!solution.contains(i)) continue

            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor(i / 9.0).toInt() * 18 + offsetY

            RenderUtils.drawRoundedRect(solverColor, currentOffsetX, currentOffsetY, 16.0, 16.0, .0)
        }

        GlStateManager.popMatrix()
    }


    private fun solve() {
        solution.clear()

        fun fixName(name: String): String {
            var fixedName = name
            replacements.forEach { (k, v) ->
                fixedName = fixedName.replace(Regex("^$k"), v)
            }
            return fixedName
        }

        slots.filter{
            it != null && allowedSlots.contains(it.num) && !it.enchanted &&
               fixName(it.name.toLowerCase()).startsWith(extra ?: "")
        }.map { it!!.num }.forEach { solution.add(it) }
    }

    private fun predict(slot: Int, button: Int) {
        solution.remove(slot)
    }


    private fun click(slot: Int, button: Int) {
        clicked = true
        mc.netHandler.addToSendQueue(C0EPacketClickWindow(cwid, slot, button, 0, null, 0))
        val initialWindowId = cwid
        setTimeout(1000) {
            if (!inTerminal || initialWindowId != cwid) return@setTimeout
            queue.clear()
            solve()
            clicked = false
        }
    }

    @SubscribeEvent
    fun onWindowOpen(event: PacketEvent.Received) {
        if (!config.CustomTerminalsGui || !config.CustomColorsTerminal || LocationUtils.dungeonFloor != 7 || F7Phase != 3) return
        if (event.packet !is S2DPacketOpenWindow) return

        val windowTitle = event.packet.windowTitle.unformattedText.removeFormatting()
        val slotCount = event.packet.slotCount
        val colorsMatch = Regex("^Select all the ([\\w ]+) items!$").matchEntire(windowTitle)
        cwid = event.packet.windowId

        if (colorsMatch != null) {
            extra = colorsMatch.groupValues[1].toLowerCase()
            inTerminal = true
            clicked = false
            slots.clear()
            windowSize = slotCount
        }
        else inTerminal = false
    }

    @SubscribeEvent
    fun onS2FPacketSetSlot(event: PacketEvent.Received) {
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
        else slots[slot] = null

        if (slots.size == windowSize) {
            solve()
            if (queue.isNotEmpty() && queue.all { solution.contains(it.first) }) {
                queue.forEach { predict(it.first, it.second) }
                click(queue[0].first, queue[0].second)
                queue.removeAt(0)
            }
            else queue.clear()
        }
    }

    @SubscribeEvent
    fun onWindowClose(event: PacketEvent.Received) {
        if (event.packet !is S2EPacketCloseWindow) return
        if (!inTerminal) return
        reset()
        AYAYA.play()
    }

    @SubscribeEvent
    fun onSentPacket(event: PacketEvent.Sent) {
        if (event.packet !is C0DPacketCloseWindow) return
        if (!inTerminal) return
        reset()
    }

    private fun reset() {
        inTerminal = false
        queue.clear()
    }
}
