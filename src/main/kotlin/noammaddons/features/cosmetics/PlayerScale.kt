package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.config
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.DungeonUtils.dungeonTeammates
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.PlayerUtils.Player

object PlayerScale {
    @SubscribeEvent
    fun onRenderEntityPre(event: RenderLivingEvent.Pre<*>) {
        if (!config.PlayerScale || event.entity !is EntityPlayer) return
        if (!config.PlayerScaleOnEveryone && event.entity != Player) return
        if (inDungeons && dungeonTeammates.none { it.entity == event.entity }) return


        GlStateManager.pushMatrix()

        GlStateManager.translate(
            event.x.toFloat(),
            event.y.toFloat(),
            event.z.toFloat()
        )

        val scaleFactor = config.PlayerScaleValue
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
        if (!config.PlayerScaleOnEveryone && event.entity != Player) return
        if (inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.popMatrix()
    }

}