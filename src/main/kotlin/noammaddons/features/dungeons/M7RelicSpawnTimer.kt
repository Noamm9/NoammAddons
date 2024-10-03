package noammaddons.features.dungeons

import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import java.awt.Color

object M7RelicSpawnTimer {
	/*
	val RelicBoxes = mapOf(
		"orange" to Pair(BlockPos(86, 6, 63).toVec3(), BlockPos(95, 10, 50).toVec3()),
		"purple" to Pair(BlockPos(61, 7, 126).toVec3(), BlockPos(50, 11, 137).toVec3()),
        "red" to Pair(BlockPos(25, 5, 65).toVec3(), BlockPos(18, 9, 54).toVec3()),
        "green" to Pair(BlockPos(26, 5, 98).toVec3(), BlockPos(15, 9, 88).toVec3()),
		"blue" to Pair(BlockPos(83, 5, 88).toVec3(), BlockPos(94, 10, 100).toVec3())
	)*/

	private var StartTimer = false
	private var ticks = 0
	
	@SubscribeEvent
	fun onPacket(event: PacketEvent.Received) {
		if (!config.M7RelicSpawnTimer) return
		
		if (event.packet is S32PacketConfirmTransaction) {
			if (!StartTimer) return
			if (ticks <= 0) return
			ticks--
		}
		
		if (event.packet !is S02PacketChat) return
		if (event.packet.chatComponent.unformattedText.removeFormatting() != "[BOSS] Necron: All this, for nothing...") return
		
		ticks = 50
		StartTimer = true
	}
	

	
	@SubscribeEvent
	fun drawTimer(event: RenderOverlay) {
		if (!config.M7RelicSpawnTimer) return
		if (ticks <= 0) return
		
		drawCenteredText(
			(ticks / 20.0).toFixed(2),
			mc.getWidth() / 2f,
			mc.getHeight() * 0.4f,
			3f, thePlayer?.clazz?.color ?: Color.WHITE,
		)
	}
}
