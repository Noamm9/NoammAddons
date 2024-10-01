package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.RenderUtils.drawTracer
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
