package noammaddons.features.general

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiContainerEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.renderItem


// known bug: cake's num in slotnum 0 renders behind the item
object CakeNumbers: Feature() {
    private val cakeRegex = Regex("New Year Cake \\(Year (\\d+)\\)")

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.DrawSlotEvent) {
        if (! config.cakeNumbers) return
        val name = event.slot.stack?.displayName?.removeFormatting() ?: return

        val (x, y) = Pair(event.slot.xDisplayPosition + 8f, event.slot.yDisplayPosition + 8f)
        val match = cakeRegex.find(name) ?: return
        val cakeStr = "§b${match.destructured.component1()}"

        event.isCanceled = true
        GlStateManager.pushMatrix()
        renderItem(event.slot.stack, x - 8, y - 8)
        GlStateManager.translate(.0, .0, 300.0)
        drawCenteredText(cakeStr, x, y, 0.8)
        GlStateManager.popMatrix()
    }
}
