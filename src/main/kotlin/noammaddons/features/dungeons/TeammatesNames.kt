package noammaddons.features.dungeons

import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.MathUtils
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.getRenderVec
import noammaddons.utils.RenderUtils.getRenderX
import noammaddons.utils.RenderUtils.getRenderY
import noammaddons.utils.RenderUtils.getRenderZ

object TeammatesNames {
    @SubscribeEvent
    fun renderNametags(event: RenderWorldLastEvent) {
	    if (!config.dungeonTeammatesNames || !inDungeons) return
	    
        dungeonTeammatesNoSelf.forEach {
            it.entity?.let { entity ->
                val distance = MathUtils.distanceIn3DWorld(Player!!.getRenderVec(), entity.getRenderVec())
                var scale = (distance * 0.135).toFloat()
                if (scale <1f) scale = 1f
	            
                RenderUtils.drawString(
                    entity.name,
                    entity.getRenderX(),
                    entity.getRenderY() + getPlayerHeight(1) + distance*0.04f,
                    entity.getRenderZ(),
                    it.clazz.color, scale,
					phase = true
                )
            }
        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Specials.Pre<*>) {
        if (!config.dungeonTeammatesNames || !inDungeons) return
	    event.isCanceled = dungeonTeammatesNoSelf.any {
			event.entity == it.entity
		}
    }
}
