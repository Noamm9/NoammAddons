package noammaddons.features.General

import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.MathUtils
import noammaddons.utils.PartyUtils
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.getPlayerHeight
import noammaddons.utils.RenderUtils.drawString
import noammaddons.utils.RenderUtils.getRenderVec
import java.awt.Color

object PartyNames {
	@SubscribeEvent
	@Suppress("UNUSED_PARAMETER")
	fun drawName(event: RenderWorldLastEvent) {
		if (!config.partyNames || inDungeons) return
		
		PartyUtils.partyMembers.forEach {
			mc.theWorld.getPlayerEntityByName(it)?.run {
				val distance = MathUtils.distanceIn3DWorld(Player!!.getRenderVec(), getRenderVec())
				var scale = (distance * 0.0835).toFloat()
				if (scale <0.7f) scale = 0.7f
				
				drawString(
					displayName.formattedText,
					getRenderVec().add(Vec3(.0, getPlayerHeight(1) + distance*0.02f, .0)),
					Color.WHITE, scale,
					renderBlackBox = false,
					shadow = true,
					phase = true
				)
			}
		}
	}
}
