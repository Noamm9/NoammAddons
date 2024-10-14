package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.RenderLivingEntityEvent
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.OutlineUtils.outlineESP
import noammaddons.utils.PartyUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.Utils.ChromaColor
import java.awt.Color
import kotlin.math.abs
import kotlin.math.sin


object PartyOutline {
    @SubscribeEvent
    fun OutlinePartyMembers(event: RenderLivingEntityEvent) {
        if (config.partyOutline && !inDungeons) {
			PartyUtils.partyMembers.forEach {
				if (event.entity.name == it) {
					outlineESP(event, ChromaColor())
				}
			}
        }
    }
}
