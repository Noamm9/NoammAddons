package noammaddons.features.misc

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.DungeonUtils.dungeonTeammates
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.Player

object PlayerScale: Feature() {

    // IDK why but this shit legit gave me a headache
    fun getPlayerScaleFactor(ent: Entity): Float {
        if (! config.PlayerScale) return 1f
        return if (config.PlayerScaleOnEveryone) config.PlayerScaleValue
        else if (ent == Player) config.PlayerScaleValue
        else 1f
    }


    @SubscribeEvent
    fun onRenderEntityPre(event: RenderPlayerEvent.Pre) {
        if (! config.PlayerScale) return
        if (! config.PlayerScaleOnEveryone && event.entity != Player) return
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
            - event.x.toFloat(),
            - event.y.toFloat(),
            - event.z.toFloat()
        )
    }

    @SubscribeEvent
    fun onRenderEntityPost(event: RenderPlayerEvent.Post) {
        if (! config.PlayerScale) return
        if (! config.PlayerScaleOnEveryone && event.entity != Player) return
        if (inDungeons && dungeonTeammates.none { it.entity == event.entity }) return

        GlStateManager.popMatrix()
    }

}