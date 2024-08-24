package NoammAddons.features.Cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object CustomPlayerScale {
    @SubscribeEvent
    fun onRenderEntityPre(event: RenderLivingEvent.Pre<*>) {
        if (!config.PlayerScale || event.entity !is EntityPlayer) return
        if (!config.PlayerScaleOnEveryone && event.entity != mc.thePlayer) return

        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()

        GlStateManager.translate(
            event.x.toFloat(),
            event.y.toFloat(),
            event.z.toFloat()
        )

        val scaleFactor = 1.0f * config.PlayerScaleValue
        GlStateManager.scale(scaleFactor, scaleFactor, scaleFactor)

        GlStateManager.translate(
            -event.x.toFloat(),
            -event.y.toFloat(),
            -event.z.toFloat()
        )
    }

    @SubscribeEvent
    fun onRenderEntityPost(event: RenderLivingEvent.Post<*>) {
        if (!config.PlayerScale || event.entity !is EntityPlayer) return
        if (!config.PlayerScaleOnEveryone && event.entity != mc.thePlayer) return

        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }

}