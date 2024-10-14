package noammaddons.features.cosmetics

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ThreadUtils.runEvery

object PlayerSpin {
	private val speedFactor get() = config.SpinSpeed / 25f
    private var rot = .0f
	private var i = 0

    init {
        runEvery(10) {
            i++

            rot =
                if (config.SpinDirection == 0) (((i * speedFactor) % 360) - 180)
                else (180 - ((i * speedFactor) % 360))
		}
	}


    @SubscribeEvent
    fun onRenderEntityPre(event: RenderLivingEvent.Pre<*>) {
        if (!config.PlayerSpin) return
        if (event.entity != Player) return
        //if (!config.SpinOnEveryone && event.entity != mc.thePlayer) return
        //if (config.SpinOnEveryone && inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.pushMatrix()
        GlStateManager.rotate(rot, 0f, 1f, 0f)
    }

    @SubscribeEvent
    fun onRenderEntityPost(event: RenderLivingEvent.Post<*>) {
        if (!config.PlayerSpin) return
        if (event.entity != Player) return
        //if (!config.SpinOnEveryone && event.entity != mc.thePlayer) return
        //if (config.SpinOnEveryone && inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.popMatrix()
    }
}
