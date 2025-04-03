package noammaddons.features.gui

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiKeybourdInputEvent
import noammaddons.features.Feature
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf

object WardrobeKeybinds: Feature() {
    private val keyMap = mapOf(
        0 to 36, 1 to 37, 2 to 38,
        3 to 39, 4 to 40, 5 to 41,
        6 to 42, 7 to 43, 8 to 44,
    )


    @SubscribeEvent
    fun onKeyInput(event: GuiKeybourdInputEvent) {
        if (! config.wardrobeKeybinds) return
        if (! ActionUtils.inWardrobeMenu) return
        if (event.keyCode.equalsOneOf(1, 18)) return

        val windowId = mc.thePlayer?.openContainer?.windowId ?: return
        val index = mc.gameSettings.keyBindsHotbar.withIndex().find {
            it.value.keyCode == event.keyCode
        }?.index ?: return

        val slot = keyMap[index] ?: return
        if (! config.wardrobeKeybindsCloseAfterUse) mc.playerController.windowClick(windowId, slot, 0, 0, mc.thePlayer)
        else {
            GuiUtils.sendWindowClickPacket(slot, 0, 0)
            PlayerUtils.closeScreen()
        }
        event.isCanceled = true
    }
}
