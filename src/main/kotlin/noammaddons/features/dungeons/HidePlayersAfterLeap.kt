package noammaddons.features.dungeons

import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils
import noammaddons.utils.ThreadUtils.setTimeout


// @Inspired by Hideshichas's CT module: Hideout
// https://github.com/Hideshichan/Hideout/blob/main/Hideout/Features/LeapHelper.js
object HidePlayersAfterLeap {
	private var HidePlayers = false
	
	@SubscribeEvent
	fun onLeap(event: Chat) {
		if (!config.hidePlayersAfterLeap) return
		if (!event.component.unformattedText
			.removeFormatting().matches(Regex("^You have teleported to .+!$"))
		) return
		debugMessage("hidePlayersAfterLeap: Triggered")
		
		HidePlayers = true
		setTimeout(3500) { HidePlayers = false }
	}
	
	@SubscribeEvent
	fun onRenderPlayer(event: RenderLivingEvent.Pre<*>) {
		if (!HidePlayers) return
		if (event.entity.entityId == mc.thePlayer.entityId) return
		
		event.isCanceled = DungeonUtils.dungeonTeammates.map { it.entity }.contains(event.entity)
	}
}
