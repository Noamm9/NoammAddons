package noammaddons.utils

import gg.essential.api.EssentialAPI
import io.github.moulberry.notenoughupdates.NEUApi
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import kotlinx.coroutines.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.*
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.mixins.AccessorGuiContainer
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ReflectionUtils.getField
import noammaddons.utils.Utils.send


object GuiUtils {
    private var isGuiHidden = false
    private var onHideAction: () -> Unit = {}
    private const val FAILSAFE_TIMEOUT = 10_000L
    private var failsafeJob: Job? = null
    var currentChestName: String = ""

    @SubscribeEvent
    fun onGuiClose(event: GuiOpenEvent) {
        currentChestName = getContainerName(event.gui)
    }

    /**
     * Sends a packet to the server simulating a click in a container's window.
     * @param slotId The ID of the slot being clicked in the container.
     * @param mouseButton The mouse button used to perform the click. 0 for left-click, 1 for right-click, 2 for middle-click.
     * @param clickType 0 - Normal click,  1 - Shift-click,  2 - Pick block,  3 - Middle-click

     */
    fun sendWindowClickPacket(slotId: Int, mouseButton: Int, clickType: Int) = mc.thePlayer?.openContainer?.run {
        C0EPacketClickWindow(
            windowId,
            slotId,
            mouseButton,
            clickType,
            inventory[slotId],
            getNextTransactionID(mc.thePlayer.inventory)
        ).send()
    }


    fun isInGui(): Boolean = mc.currentScreen != null

    fun getContainerName(gui: GuiScreen? = mc.currentScreen): String {
        val chestGui = gui as? GuiChest ?: return ""
        val container = chestGui.inventorySlots as? ContainerChest ?: return ""
        return container.lowerChestInventory.displayName.formattedText
    }

    fun changeTitle(newTitle: String, gui: GuiScreen? = mc.currentScreen) {
        val title = newTitle.addColor()
        val guiChest = gui as? GuiChest ?: return

        val field =
            getField(guiChest, "lowerChestInventory") as? InventoryBasic
                ?: getField(guiChest, "field_147015_w") as? InventoryBasic
                ?: return
        if (field.displayName?.formattedText == "$title§r") return

        field.setCustomName(title)
    }

    fun disableNEUInventoryButtons() {
        if (Loader.instance().activeModList.any { it.modId == NotEnoughUpdates.MODID }) {
            NEUApi.setInventoryButtonsToDisabled()
        }
    }

    fun openScreen(screen: GuiScreen?) {
        EssentialAPI.getGuiUtil().openScreen(screen)
    }

    fun getSlotFromIndex(slotIndex: Int?): Slot? {
        return slotIndex?.let {
            val con = (mc.currentScreen as? GuiContainer) ?: return null
            con.inventorySlots?.inventorySlots?.get(it)
        }
    }

    fun getSlotRenderPos(slotIndex: Int?): Pair<Float, Float>? {
        val gui = (mc.currentScreen as? AccessorGuiContainer) ?: return null
        val slot = getSlotFromIndex(slotIndex) ?: return null
        val x = gui.guiLeft + slot.xDisplayPosition
        val y = gui.guiTop + slot.yDisplayPosition
        return Pair(x.toFloat(), y.toFloat())
    }

    fun hideGui(bool: Boolean, callback: () -> Unit = {}) {
        isGuiHidden = bool
        onHideAction = callback

        if (bool) startFailsafe()
        else cancelFailsafe()
    }

    private fun startFailsafe() {
        failsafeJob?.cancel()
        failsafeJob = CoroutineScope(Dispatchers.Default).launch {
            delay(FAILSAFE_TIMEOUT)
            isGuiHidden = false
            onHideAction = {}
        }
    }

    private fun cancelFailsafe() {
        failsafeJob?.cancel()
        failsafeJob = null
    }

    @SubscribeEvent
    fun INTERNAL_RenderShit(event: RenderOverlay) {
        if (isGuiHidden) onHideAction()
        else onHideAction = {}
    }

    @SubscribeEvent
    fun INTERNAL_HideGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        event.isCanceled = isGuiHidden
    }

    @SubscribeEvent
    fun INTERNAL_CancelKeyboardInputs(event: GuiScreenEvent.KeyboardInputEvent.Pre) {
        event.isCanceled = isGuiHidden
    }

    @SubscribeEvent
    fun INTERNAL_CancelMouseInputs(event: GuiScreenEvent.MouseInputEvent.Pre) {
        event.isCanceled = isGuiHidden
    }

    @SubscribeEvent
    fun INTERNAL_CancelButtonInputs(event: GuiScreenEvent.ActionPerformedEvent.Pre) {
        event.isCanceled = isGuiHidden
    }

}
