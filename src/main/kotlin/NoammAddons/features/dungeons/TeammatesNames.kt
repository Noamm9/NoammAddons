package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.DungeonUtils
import NoammAddons.utils.LocationUtils.inSkyblock
import NoammAddons.utils.MathUtils
import NoammAddons.utils.RenderUtils
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TeammatesNames {
    @SubscribeEvent
    fun renderNametags(event: RenderWorldLastEvent) {
        if (!inSkyblock || !config.dungeonTeammatesNames) return

        val partialTicks = event.partialTicks

        DungeonUtils.dungeonTeammatesNoSelf.forEach { DungeonPlayer ->
            DungeonPlayer.entity?.let { entity ->
                val distance = MathUtils.distanceIn3DWorld(mc.thePlayer.positionVector, entity.positionVector)
                var scale = (distance * 0.2).toFloat()
                if (distance <10) scale = 2f

                val RenderPosX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
                val RenderPosY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
                val RenderPosZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks

                RenderUtils.drawString(
                    entity.name,
                    Vec3(
                        RenderPosX,
                        RenderPosY + 3 + distance*0.01f,
                        RenderPosZ
                    ),
                    DungeonPlayer.clazz.color,
                    scale
                )
            }
        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Specials.Pre<*>) {
        if (!inSkyblock || !config.dungeonTeammatesNames) return
        DungeonUtils.dungeonTeammatesNoSelf.forEach { DungeonPlayer ->
            if (event.entity == DungeonPlayer.entity) event.isCanceled = true
        }
    }
}
