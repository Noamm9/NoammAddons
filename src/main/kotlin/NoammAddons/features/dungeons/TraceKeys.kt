package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.LocationUtils.inBoss
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.RenderUtils.drawTracer
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object TraceKeys {
    @SubscribeEvent
    fun TraceKeys(event: RenderWorldLastEvent) {
        if (config.TraceKeys && !inBoss && inDungeons) {
            for (entity in mc.theWorld.getLoadedEntityList()) {
                when (entity.displayName.unformattedText.removeFormatting()) {
                    "Wither Key" -> drawTracer(entity.positionVector.add(Vec3(0.0, 1.7, 0.0)), Color(0, 0, 0))
                    "Blood Key" -> drawTracer(entity.positionVector.add(Vec3(0.0, 1.7, 0.0)), Color(255, 0, 0))
                }
            }
        }
    }
}
