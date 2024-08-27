package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.LocationUtils
import NoammAddons.utils.RenderUtils.drawText
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent

object EnergyCrystal {
    private var showEnergyCrystal = false
    private val text = "&e&l⚠ &l&bCrystal &e&l⚠ ".addColor()

    @SubscribeEvent
    fun checkINV(event: ServerTickEvent) {
        showEnergyCrystal = if (LocationUtils.F7Phase == 1 && mc.thePlayer != null && config.energyCrystalAlert) {
            mc.thePlayer?.inventory?.mainInventory?.get(8)?.displayName?.removeFormatting() == "Energy Crystal"
        } else false
    }

    @SubscribeEvent
    fun title(event: RenderGameOverlayEvent.Post) {
        if (!showEnergyCrystal || mc.ingameGUI == null || event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        drawText(
            text,
            (ScaledResolution(mc).scaledWidth) / 2 - (mc.fontRendererObj.getStringWidth(text.removeFormatting()) * 4.5) / 2,
            ScaledResolution(mc).scaledHeight / 2 - 20 * 4.5,
            4.5
        )
    }
}