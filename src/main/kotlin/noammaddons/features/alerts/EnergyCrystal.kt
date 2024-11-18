package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ItemUtils.getItemIndexInHotbar
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.Utils.isNull

object EnergyCrystal: Feature() {
    @SubscribeEvent
    fun title(event: RenderOverlay) {
        if (! config.energyCrystalAlert) return
        if (getItemIndexInHotbar("Energy Crystal").isNull()) return

        drawCenteredText("&e&l⚠ &l&bCrystal &e&l⚠", mc.getWidth() / 2f, mc.getHeight() * 0.4f, 3.5f)
    }
}