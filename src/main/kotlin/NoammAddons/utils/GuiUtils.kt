package NoammAddons.utils

import kotlinx.coroutines.delay
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.GuiContainerEvent
import NoammAddons.utils.ChatUtils.removeFormatting

object GuiUtils {
    var currentChestName: String? = null

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        currentChestName = getChestName(event.gui)
    }

    @SubscribeEvent
    fun OnGuiClose(event: GuiContainerEvent.CloseEvent) {
        currentChestName = null
    }

     fun clickSlot(slot: Int, shift: Boolean = false, click: String = "LEFT") {
        if (mc.thePlayer.openContainer.inventorySlots.size < slot || currentChestName == "") {
            throw IndexOutOfBoundsException()
        }
         val clickString = click.removeFormatting().toLowerCase()
         var clickType = 1
         if (clickString == "LEFT") clickType = 1
         if (clickString == "RIGHT") clickType = 2
         if (clickString == "MIDDLE") clickType = 0


        mc.playerController.windowClick(
            mc.thePlayer.openContainer.windowId,
            slot,
            clickType,
            if (shift) 1 else 0,
            mc.thePlayer
        )
        return
    }

     fun getChestName(gui: GuiScreen?): String {
        return if (gui is GuiChest) {
            (gui.inventorySlots as? ContainerChest)?.lowerChestInventory?.displayName?.unformattedText ?: ""
        }
        else ""
    }

    fun isInGui(): Boolean = currentChestName != null
}
