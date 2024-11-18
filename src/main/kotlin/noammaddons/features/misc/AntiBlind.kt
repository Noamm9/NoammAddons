package noammaddons.features.misc

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inSkyblock

object AntiBlind: Feature() {

    @SubscribeEvent
    fun onRenderFog(event: FogDensity) {
        if (! config.antiBlind || ! inSkyblock) return
        event.density = 0f
        GlStateManager.setFogStart(998f)
        GlStateManager.setFogEnd(999f)
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Pre) {
        if (! config.antiPortal) return
        if (! inSkyblock) return
        if (event.type != RenderGameOverlayEvent.ElementType.PORTAL) return

        event.isCanceled = true
    }
}
