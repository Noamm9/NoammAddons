package noammaddons.features.dungeons

import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MathUtils
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderHelper.renderX
import noammaddons.utils.RenderHelper.renderY
import noammaddons.utils.RenderHelper.renderZ
import noammaddons.utils.RenderUtils

object TeammatesNames: Feature() {
    @SubscribeEvent
    fun onRenderNameTag(event: RenderLivingEvent.Specials.Pre<*>) {
        if (! config.dungeonTeammatesNames || ! inDungeon) return
        val teammate = dungeonTeammatesNoSelf.find { it.entity?.entityId == event.entity.entityId } ?: return
        val distance = MathUtils.distance3D(mc.thePlayer.renderVec, event.entity.renderVec)
        var scale = (distance * 0.135).toFloat()
        if (scale < 1f) scale = 1f
        event.isCanceled = true

        RenderUtils.drawString(
            event.entity.name,
            event.entity.renderX,
            event.entity.renderY + getPlayerHeight(event.entity, 1) + distance * 0.015f,
            event.entity.renderZ,
            teammate.clazz.color,
            scale, phase = true
        )
    }
}
