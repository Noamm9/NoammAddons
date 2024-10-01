package noammaddons.features.dungeons

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.toVec3
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.RenderUtils
import java.awt.Color

object M7RelicOutline {
	private data class RelicCauldron(val pos: BlockPos, val color: Color)
	private val RelicCauldrons = mapOf(
		"Corrupted Blue Relic" to RelicCauldron(BlockPos(59, 7, 44), Color(0,138,255,110)),
		"Corrupted Orange Relic" to RelicCauldron(BlockPos(57, 7, 42), Color(255,114,0,110)),
		"Corrupted Purple Relic" to RelicCauldron(BlockPos(54, 7, 41), Color(129, 0, 111, 110)),
		"Corrupted Red Relic" to RelicCauldron(BlockPos(51, 7, 42), Color(255, 0, 0, 110)),
        "Corrupted Green Relic" to RelicCauldron(BlockPos(49, 7, 44), Color(0, 255, 0, 110))
	)
	
	@SubscribeEvent
	fun RelicOutline(event: RenderWorldLastEvent) {
		if (!config.M7RelicOutline) return
		if (F7Phase != 5) return
		
		RelicCauldrons[mc.thePlayer?.inventory?.mainInventory?.get(8)?.displayName.removeFormatting()]?.run {
			RenderUtils.drawBlockBox(pos, color, outline = true, fill = true, phase = true)
			RenderUtils.drawTracer(pos.toVec3().add(Vec3(0.5, 0.5, 0.5)), color)
		}
	}
	
}