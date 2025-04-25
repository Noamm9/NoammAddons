package noammaddons.features.impl.esp

import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.features.impl.esp.GlobalEspSettings.phase
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.EspUtils.espMob
import noammaddons.utils.LocationUtils
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.PartyUtils
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawString

object PartyESP: Feature("Highlight party members in the world") {
    private val drawNames = ToggleSetting("Draw Name", true)
    private val drawHighlight = ToggleSetting("Highlight", true)
    override fun init() = addSettings(drawNames, drawHighlight)

    private fun getPartyNoSelf() = PartyUtils.entities.filter { it != mc.thePlayer }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Specials.Pre<*>) {
        if (! drawHighlight.value && ! drawNames.value) return
        if (LocationUtils.inDungeon) return
        val member = getPartyNoSelf().find { it.entityId == event.entity.entityId } ?: return
        if (drawHighlight.value) espMob(event.entity, getRainbowColor(1f))
        val distance = distance3D(mc.thePlayer.renderVec, member.renderVec)
        var scale = (distance * 0.0875).toFloat()
        if (scale < 0.7f) scale = 0.7f
        if (! drawNames.value) return
        event.isCanceled = true

        drawString(
            member.displayName.formattedText,
            member.renderVec.add(y = getPlayerHeight(member, 1) + distance * 0.02f),
            phase = phase,
            scale = scale
        )
    }
}