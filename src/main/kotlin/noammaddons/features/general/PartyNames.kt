package noammaddons.features.general

import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.PartyUtils.partyMembersNoSelf
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawString

object PartyNames: Feature() {
    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Specials.Pre<*>) {
        if (! config.partyNames || inDungeon) return
        val member = partyMembersNoSelf.find { it.second?.entityId == event.entity.entityId } ?: return
        val distance = distance3D(mc.thePlayer.renderVec, member.second?.renderVec ?: return)
        var scale = (distance * 0.0875).toFloat()
        if (scale < 0.7f) scale = 0.7f
        event.isCanceled = true

        drawString(
            member.second !!.displayName.formattedText,
            member.second !!.renderVec.add(y = getPlayerHeight(member.second !!, 1) + distance * 0.02f),
            scale = scale,
            phase = true
        )
    }
}
