package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText

object EnergyCrystal: Feature() {
    @SubscribeEvent
    fun title(event: RenderOverlay) {
        if (! config.energyCrystalAlert) return
        if (LocationUtils.F7Phase != 1) return
        if (mc.thePlayer.inventory.getStackInSlot(8)?.displayName?.removeFormatting()?.lowercase() != "energy crystal") return

        drawCenteredText("&e&l⚠ &l&bCrystal &e&l⚠", mc.getWidth() / 2, mc.getHeight() * 0.2, 3)
    }
}