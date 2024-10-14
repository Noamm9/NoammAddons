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
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.getRenderVec
import noammaddons.utils.RenderUtils.getRenderX
import noammaddons.utils.RenderUtils.getRenderY
import noammaddons.utils.RenderUtils.getRenderZ
import noammaddons.utils.Utils.isNull
import java.awt.Color

object TraceKeys {
    @SubscribeEvent
    fun TraceKeys(event: RenderWorldLastEvent) {
        if (config.TraceKeys && !inBoss && inDungeons) {
            for (entity in mc.theWorld.getLoadedEntityList()) {
                when (entity.displayName.unformattedText.removeFormatting()) {
                    "Wither Key" -> {
                        drawTracer(
                            entity.getRenderVec().add(Vec3(.0, 1.7, .0)),
                            Color.BLACK
                        )
                        drawBox(
                            entity.getRenderX() - 0.4,
                            entity.getRenderY() + 1.7f - 0.4,
                            entity.getRenderZ() - 0.4,
                            Color(0, 0, 0, 85),
                            outline = true,
                            fill = true,
                            width = 0.8,
                            height = 0.8,
                            phase = true,
                        )
                    }
                    "Blood Key" -> {
                        drawTracer(
                            entity.getRenderVec().add(Vec3(.0, 1.7, .0)),
                            Color.RED
                        )
                        drawBox(
                            entity.getRenderX() - 0.4,
                            entity.getRenderY() + 1.7f - 0.4,
                            entity.getRenderZ() - 0.4,
                            Color(255, 0, 0, 85),
                            outline = true,
                            fill = true,
                            width = 0.8,
                            height = 0.8,
                            phase = true,
                        )
                    }
                }
            }
        }
    }
}
