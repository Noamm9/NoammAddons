package NoammAddons.features.cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.DungeonUtils.dungeonTeammates
import NoammAddons.utils.LocationUtils.inDungeons
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PlayerSpin {
    private var rot = .0f
    private var i = 0


    @SubscribeEvent
    fun onRenderEntityPre(event: RenderLivingEvent.Pre<*>) {
        if (!config.PlayerSpin || event.entity !is EntityPlayer) return
        if (event.entity != mc.thePlayer) return
      //  if (!config.SpinOnEveryone && event.entity != mc.thePlayer) return
        //if (config.SpinOnEveryone && inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.pushMatrix()

        i++
        val speedFactor = config.SpinSpeed / 25f
        rot = if (config.SpinDiraction == 0) (((i * speedFactor) % 360) - 180)
        else (180 - ((i * speedFactor) % 360))

        GlStateManager.rotate(rot, 0f, 1f, 0f)

    }

    @SubscribeEvent
    fun onRenderEntityPost(event: RenderLivingEvent.Post<*>) {
        if (!config.PlayerSpin || event.entity !is EntityPlayer) return
        if (event.entity != mc.thePlayer) return
      //  if (!config.SpinOnEveryone && event.entity != mc.thePlayer) return
       // if (config.SpinOnEveryone && inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.popMatrix()
    }
}
