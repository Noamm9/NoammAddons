package noammaddons.features.general

import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.MathUtils
import noammaddons.utils.PartyUtils.partyMembersNoSelf
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderUtils.drawString
import java.awt.Color

object PartyNames: Feature() {
    @SubscribeEvent
    fun drawName(event: RenderWorld) {
        if (! config.partyNames || inDungeons) return
        partyMembersNoSelf.filterNotNull().forEach {
            val distance = MathUtils.distanceIn3DWorld(Player?.getRenderVec() ?: return@forEach, it.second?.getRenderVec() ?: return@forEach)
            var scale = (distance * 0.0875).toFloat()
            if (scale < 0.7f) scale = 0.7f

            drawString(
                it.second !!.displayName.formattedText,
                it.second !!.getRenderVec().add(Vec3(.0, getPlayerHeight(it.second !!, 1) + distance * 0.02f, .0)),
                Color.WHITE, scale,
                renderBlackBox = false,
                shadow = true,
                phase = true
            )
        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Specials.Pre<*>) {
        if (! config.partyNames || inDungeons) return
        event.isCanceled = partyMembersNoSelf.any {
            event.entity == it.second
        }
    }
}
