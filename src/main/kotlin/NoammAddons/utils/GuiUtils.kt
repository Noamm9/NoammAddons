package NoammAddons.utils

import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.GuiContainerEvent
import NoammAddons.events.ReceivePacketEvent
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraft.network.play.server.S2DPacketOpenWindow
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import gg.essential.api.EssentialAPI
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.network.play.server.S2EPacketCloseWindow
import org.lwjgl.input.Mouse


object GuiUtils {
    private var PatcherScale = 5
    var currentChestName: String = ""

    @SubscribeEvent
    fun onPacketReceived(event: ReceivePacketEvent) {
        currentChestName = getCurrentOpenContainerName()
    }


    fun clickSlot(slot: Int, shift: Boolean = false, click: Any = "LEFT") {
         val clickString = click.toString().removeFormatting().toLowerCase()
         var clickType = 1
         if (clickString == "LEFT" || click == 1) clickType = 1
         if (clickString == "RIGHT" || click == 2) clickType = 2
         if (clickString == "MIDDLE" || click == 0) clickType = 0


        mc.playerController.windowClick(
            mc.thePlayer.openContainer.windowId,
            slot,
            clickType,
            if (shift) 1 else 0,
            mc.thePlayer
        )
        return
    }

    fun isInGui(): Boolean = mc.currentScreen != null

    fun getPatcherScale(): Int {
        return try {
            Class.forName("club.sk1er.patcher.config.PatcherConfig")
            .getDeclaredField("inventoryScale")
            .get(null) as Int
        }
        catch (_: Exception) { 1 }
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

    fun Minecraft.openScreen(screen: GuiScreen?) {
        EssentialAPI.getGuiUtil().openScreen(screen)
    }
}
