package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.DungeonUtils
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.MathUtils
import noammaddons.utils.RenderUtils
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.RenderUtils.getRenderX
import noammaddons.utils.RenderUtils.getRenderY
import noammaddons.utils.RenderUtils.getRenderZ

object TeammatesNames {
    @SubscribeEvent
    fun renderNametags(event: RenderWorldLastEvent) {
	    if (!inSkyblock || !config.dungeonTeammatesNames || ! inDungeons) return
	    
        dungeonTeammatesNoSelf.forEach {
            it.entity?.let { entity ->
                val distance = MathUtils.distanceIn3DWorld(mc.thePlayer.positionVector, entity.positionVector)
                var scale = (distance * 0.2).toFloat()
                if (distance <10) scale = 2f
	            
                RenderUtils.drawString(
                    entity.name,
                    Vec3(
                        entity.getRenderX().toDouble(),
                        entity.getRenderY() + 3 + distance*0.01f,
                        entity.getRenderZ().toDouble(),
                    ),
                    it.clazz.color, scale
                )
            }
        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Specials.Pre<*>) {
        if (!inSkyblock || !config.dungeonTeammatesNames || ! inDungeons) return
	    event.isCanceled = dungeonTeammatesNoSelf.any {
			event.entity == it.entity
		}
    }
}
