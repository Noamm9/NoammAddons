package noammaddons.features.dungeons.esp

import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.*
import noammaddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MathUtils.add
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderHelper.renderVec


object TeammatesESP: Feature() {
    @SubscribeEvent
    fun onRenderNameTag(event: RenderLivingEvent.Specials.Pre<*>) {
        if (! config.dungeonTeammatesNames || ! inDungeon) return
        val teammate = dungeonTeammatesNoSelf.find { it.entity?.entityId == event.entity.entityId } ?: return
        val distance = MathUtils.distance3D(mc.thePlayer.renderVec, event.entity.renderVec).toFloat()
        val scale = (distance * 0.115f).coerceAtLeast(1f)
        event.isCanceled = true

        EspUtils.espMob(event.entity, teammate.clazz.color)

        RenderUtils.drawBackgroundedString(
            "&e[${teammate.clazz.name[0]}]&r ${teammate.name}",
            event.entity.renderVec.add(y = getPlayerHeight(event.entity, 1) + distance * 0.015f),
            scale, phase = true, accentColor = teammate.clazz.color, textColor = teammate.clazz.color
        )
    }
}


