package noammaddons.features.dungeons

import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderHelper.getRenderX
import noammaddons.utils.RenderHelper.getRenderY
import noammaddons.utils.RenderHelper.getRenderZ
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawTracer
import java.awt.Color

object TraceKeys: Feature() {
    @SubscribeEvent
    fun TraceKeys(event: RenderWorld) {
        if (! config.TraceKeys || inBoss) return
        for (entity in mc.theWorld.loadedEntityList) {
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
                        Color(0, 0, 0, 60),
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
                        Color(255, 0, 0, 60),
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
