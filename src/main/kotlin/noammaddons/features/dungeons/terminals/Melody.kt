@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package noammaddons.features.dungeons.terminals

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.sounds.AYAYA
import noammaddons.events.GuiContainerEvent
import noammaddons.events.PacketEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.dungeons.terminals.ConstantsVeriables.getTermScale
import noammaddons.features.dungeons.terminals.ConstantsVeriables.Slot
import noammaddons.features.dungeons.terminals.ConstantsVeriables.MelodyTitle
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.clickSlot
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.LocationUtils
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.client.event.GuiScreenEvent
import noammaddons.features.dungeons.terminals.ConstantsVeriables.getColorMode
import noammaddons.features.dungeons.terminals.ConstantsVeriables.getSolutionColor
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY
import noammaddons.utils.LocationUtils.F7Phase
import java.awt.Color
import kotlin.math.floor

object Melody {
    private var cwid = -1;
    private val slots = mutableListOf<Slot?>()
    private var windowSize = 0;
    private var isInTerminal = false
    private var correct: Int = -1
    private var button: Int = -1
    private var current: Int = -1


    @SubscribeEvent
    fun onGuiClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (!isInTerminal) return
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

        val offsetX = screenWidth / 2 - width / 2 + globalOffsetX
        val offsetY = screenHeight / 2 - height / 2 + globalOffsetY

        val slotX = floor((x - offsetX) / 18).toInt()
        val slotY = floor((y - offsetY) / 18).toInt()

        if (slotX < 0 || slotX > 8 || slotY < 0) return

        val slot = slotX + slotY * 9

        if (slot >= windowSize) return

        if (intArrayOf(16, 25, 34, 43).contains(slot)) {
            clickSlot(slot, false, 0)
            if (config.DevMode) modMessage("clicked slot $slot")
        }
    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (!isInTerminal) return
        if (!config.DevMode) event.isCanceled = true

        val termScale = getTermScale()
        val screenWidth = mc.getWidth() / termScale
        val screenHeight = mc.getHeight() / termScale

        val width = 9f * 18f
        val height = windowSize / 9f * 18f

        val globalOffsetX = 0f
        val globalOffsetY = 0f

        val offsetX = screenWidth / 2f - width / 2f + globalOffsetX
        val offsetY = screenHeight / 2f - height / 2f + globalOffsetY

        val colorMode = getColorMode()
        val solverColor = getSolutionColor()

        GlStateManager.pushMatrix()
        GlStateManager.scale(termScale, termScale, 0f)


        RenderUtils.drawRoundedRect(
            colorMode.darker(),
            offsetX - 2f - (width / 15f) / 2f,
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

        RenderUtils.drawRoundedRect(Color(255, 0, 255), offsetX + (correct + 1) * 18, offsetY + 18, 16f, 70f, 0f)
        mc.fontRendererObj.drawStringWithShadow(MelodyTitle, offsetX.toFloat(), offsetY.toFloat(), Color(255, 255, 255).rgb)

        for (i in 0 until windowSize) {
            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor((i / 9f)) * 18f + offsetY

            val buttonSlot = button * 9 + 16
            val currentSlot = button * 9 + 10 + current

            when {
                i == buttonSlot -> RenderUtils.drawRoundedRect(solverColor, currentOffsetX, currentOffsetY, 16f, 16f, 0f)
                intArrayOf(16, 25, 34, 43).contains(i) -> RenderUtils.drawRoundedRect(Color.RED, currentOffsetX, currentOffsetY, 16f, 16f, 0f)
                i == currentSlot -> RenderUtils.drawRoundedRect(Color(255, 116, 0), currentOffsetX, currentOffsetY, 16f, 16f, 0f)
            }
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onS2FPacketSetSlot(event: PacketEvent.Received) {
        if (!isInTerminal) return
        if (event.packet !is S2FPacketSetSlot) return

        val itemStack = event.packet.func_149174_e()
        val slot = event.packet.func_149173_d()

        if (slot < 0) return
        if (slot >= windowSize) return

        if (itemStack !== null) {
            slots[slot] = Slot(
                slot,
                itemStack.getItemId(),
                itemStack.metadata,
                itemStack.stackSize,
                itemStack.displayName.removeFormatting(),
                itemStack.isItemEnchanted,
            )


            if (slots[slot] != null) {
                if (slots[slot]!!.id === 160 && slots[slot]!!.meta === 5) {
                    val correct1 = slots.find { (it?.id ?: 0) == 160 && (it?.meta ?: 0) === 2 }?.num?.minus(1);
                    val button1 = floor((slot / 9).toDouble()) - 1;
                    val current1 = slot % 9 - 1;
                    if (correct1 != null) correct = correct1
                    button = button1.toInt();
                    current = current1
                }
            }
        }
        else slots[slot] = null
    }

    @SubscribeEvent
    fun onWindowOpen(event: PacketEvent.Received) {
        if (!config.CustomTerminalsGui || !config.CustomMelodyTerminal || LocationUtils.dungeonFloor != 7 || F7Phase != 3) return
        if (event.packet !is S2DPacketOpenWindow) return

        val windowTitle = event.packet.windowTitle.unformattedText.removeFormatting()
        val slotCount = event.packet.slotCount
        cwid = event.packet.windowId

        if (!(windowTitle.matches(Regex("^Click the button on time!$")))) return

        slots.clear()
        repeat(slotCount) { slots.add(null) }
        windowSize = slotCount
        isInTerminal = true
    }

    @SubscribeEvent
    fun onWindowClose(event: PacketEvent.Received) {
        if (event.packet !is S2EPacketCloseWindow) return
        if (!isInTerminal) return
        isInTerminal = false
        AYAYA.play()
    }

    @SubscribeEvent
    fun onSentPacket(event: PacketEvent.Sent) {
        if (event.packet !is C0DPacketCloseWindow) return
        if (!isInTerminal) return
        isInTerminal = false
    }
}
