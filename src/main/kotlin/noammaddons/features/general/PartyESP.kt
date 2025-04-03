package noammaddons.features.general

import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.EspUtils.espMob
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.PartyUtils
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawString

object PartyESP: Feature() {
    private fun getPartyNoSelf() = PartyUtils.entities.filter { it != mc.thePlayer }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Specials.Pre<*>) {
        if (! config.partyNames || inDungeon) return
        val member = getPartyNoSelf().find { it.entityId == event.entity.entityId } ?: return
        val distance = distance3D(mc.thePlayer.renderVec, member.renderVec)
        var scale = (distance * 0.0875).toFloat()
        if (scale < 0.7f) scale = 0.7f
        event.isCanceled = true
        espMob(event.entity, getRainbowColor(1f))

        drawString(
            member.displayName.formattedText,
            member.renderVec.add(y = getPlayerHeight(member, 1) + distance * 0.02f),
            scale = scale,
            phase = true
        )
    }
}
