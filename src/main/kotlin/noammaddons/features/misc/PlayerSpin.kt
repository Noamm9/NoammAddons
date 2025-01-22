package noammaddons.features.misc

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ThreadUtils.loop

object PlayerSpin: Feature() {
    private val speedFactor get() = config.SpinSpeed / 25f
    private var rot = .0f
    private var i = 0

    init {
        loop(10) {
            i ++

            rot =
                if (config.SpinDirection == 0) (((i * speedFactor) % 360) - 180)
                else (180 - ((i * speedFactor) % 360))
        }
    }


    @SubscribeEvent
    fun onRenderEntityPre(event: RenderPlayerEvent.Pre) {
        if (! config.PlayerSpin) return
        if (event.entity != Player) return
        //if (!config.SpinOnEveryone && event.entity != mc.thePlayer) return
        //if (config.SpinOnEveryone && inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.pushMatrix()
        GlStateManager.rotate(rot, 0f, 1f, 0f)
    }

    @SubscribeEvent
    fun onRenderEntityPost(event: RenderPlayerEvent.Post) {
        if (! config.PlayerSpin) return
        if (event.entity != Player) return
        //if (!config.SpinOnEveryone && event.entity != mc.thePlayer) return
        //if (config.SpinOnEveryone && inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.popMatrix()
    }
}
