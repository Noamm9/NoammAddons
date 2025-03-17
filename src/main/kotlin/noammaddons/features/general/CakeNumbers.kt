package noammaddons.features.general

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.mixins.AccessorGuiContainer
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.RenderUtils.drawCenteredText


object CakeNumbers: Feature() {
    private val cakeRegex = Regex("New Year Cake \\(Year (\\d+)\\)")

    @SubscribeEvent
    fun onGuiRender(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (! config.cakeNumbers) return
        if (currentChestName != "New Year Cake Bag§r") return
        val gui = event.gui as AccessorGuiContainer

        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, 0f, 300f)

        mc.thePlayer.openContainer.inventorySlots.forEach {
            if (it.stack?.item != Items.cake) return@forEach
            val year = cakeRegex.find(it.stack.displayName.removeFormatting())?.destructured?.component1() ?: return
            val (x, y) = Pair(
                it.xDisplayPosition + 8f + gui.guiLeft,
                it.yDisplayPosition + 8f + gui.guiTop
            )

            drawCenteredText("§b$year", x, y, 0.8)
        }

        GlStateManager.popMatrix()
    }
}
