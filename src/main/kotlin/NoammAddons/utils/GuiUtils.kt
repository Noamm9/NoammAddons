package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.PacketEvent
import NoammAddons.mixins.AccessorContainer
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import gg.essential.api.EssentialAPI
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Mouse


object GuiUtils {
    private var PatcherScale = 5
    var currentChestName: String = ""

    @SubscribeEvent
    fun onPacketReceived(event: PacketEvent.Received) {
        currentChestName = getCurrentOpenContainerName()
    }


    fun clickSlot(slot: Int, shift: Boolean = false, click: Any = "LEFT") {
         val clickString = click.toString().removeFormatting().toLowerCase()
         var clickType = 1
         if (clickString == "left" || click == "1") clickType = 1
         if (clickString == "right" || click == "2") clickType = 2
         if (clickString == "middle" || click == "0") clickType = 0


        mc.playerController.windowClick(
            mc.thePlayer.openContainer.windowId,
            slot,
            clickType,
            if (shift) 1 else 0,
            mc.thePlayer
        )
        return
    }


    /**
     * Sends a packet to the server simulating a click in a container's window.
     * @param slotId The ID of the slot being clicked in the container.
     * @param mouseButton The mouse button used to perform the click. 0 for left-click, 1 for right-click, 2 for middle-click.
     * @param clickType 0 - Normal click,  1 - Shift-click,  2 - Pick block,  3 - Middle-click

     */
    fun sendClickWindowPacket(slotId: Int, mouseButton: Int, clickType: Int) {
        mc.netHandler.addToSendQueue(
            C0EPacketClickWindow(
                mc.thePlayer.openContainer.windowId,
                slotId,
                mouseButton,
                clickType,
                mc.thePlayer.openContainer.inventory[slotId],
                (mc.thePlayer.openContainer as AccessorContainer).transactionID
            )
        )
    }



    fun isInGui(): Boolean = mc.currentScreen != null

    fun getPatcherScale(configValue: Boolean = false): Double {
        try {
            val scale = Class.forName("club.sk1er.patcher.config.PatcherConfig").getDeclaredField("inventoryScale").get(null) as Int
            return if (configValue) scale.toDouble()
            else when (scale) {
                0 -> 1.0
                1 -> 0.5
                2 -> 1.0
                3 -> 1.5
                4 -> 2.0
                5 -> 2.0
                else -> 1.0
            }
        }
        catch (_: Exception) { return 1.0 }
    }

    fun disablePatcherScale() {
        try {
            val patcherConfigClass = Class.forName("club.sk1er.patcher.config.PatcherConfig")
            val inventoryScaleField = patcherConfigClass.getDeclaredField("inventoryScale")
            inventoryScaleField.isAccessible = true
            PatcherScale = inventoryScaleField.get(null) as Int
            inventoryScaleField.set(null, 0)
        } catch (_: Exception) {}
    }

    fun enablePatcherScale() {
        try {
            val patcherConfigClass = Class.forName("club.sk1er.patcher.config.PatcherConfig")
            val inventoryScaleField = patcherConfigClass.getDeclaredField("inventoryScale")
            inventoryScaleField.isAccessible = true
            inventoryScaleField.set(null, PatcherScale)
        } catch (_: Exception) {}
    }

    fun Minecraft.getMouseX(): Float {
        val mx = Mouse.getX().toFloat()
        val rw = this.getWidth().toFloat()
        val dw = this.displayWidth.toFloat()
        return mx * rw / dw
    }

    fun Minecraft.getMouseY(): Float {
        val my = Mouse.getY().toFloat()
        val rh = this.getHeight().toFloat()
        val dh = this.displayHeight.toFloat()
        return rh - my * rh / dh - 1f
    }


    private fun getCurrentOpenContainerName(): String {
        val currentScreen = mc.currentScreen

        if (currentScreen is GuiChest) {
            val chestContainer = currentScreen.inventorySlots

            if (chestContainer is ContainerChest) {
                val chestInventory = chestContainer.lowerChestInventory

                return chestInventory.displayName.unformattedText
            }
        }
        return ""
    }

    fun openScreen(screen: GuiScreen?) {
        EssentialAPI.getGuiUtil().openScreen(screen)
    }
}
