package noammaddons.features.dungeons

import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderLivingEntityEvent
import noammaddons.events.Tick
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.OutlineUtils.outlineESP
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.ScanUtils.ScanRoom.currentRoom
import noammaddons.utils.ScanUtils.ScanRoom.getRoomCenter


// fuck this shit sorting not working
object BlazeSolver {
	private val BlazeHpRegex = Regex("""^\[Lv15] Blaze [\d,]+/([\d,]+)❤$""")
	private val blazes = mutableListOf<Entity>()
	private var blazeStarted: Long? = null
	private var trueTimeStarted: Long? = null
	private var lastBlazeCount = 10
	private val hpMap = mutableMapOf<Entity, Int>()
	
	@SubscribeEvent
	@Suppress("UNUSED_PARAMETER")
	fun solveBlaze(event: Tick) {
		if (!config.BlazeSolver || !inDungeons || inBoss) return
		if (currentRoom?.name != "Blaze") {
			blazes.clear()
			hpMap.clear()
			blazeStarted = null
			trueTimeStarted = null
			lastBlazeCount = 10
			return
		}
		
		blazes.clear()
		hpMap.clear()
		
		mc.theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>().forEach { e ->
			val match = BlazeHpRegex.find(e.displayName.unformattedText.removeFormatting()) ?: return@forEach
			val health = match.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach
			
			val possibleEntity = e.entityWorld.getEntitiesInAABBexcluding(
				e, e.entityBoundingBox.offset(0.0, -1.0, 0.0)
			) { it is EntityBlaze }.find { it is EntityBlaze } as? EntityBlaze
			
			if (possibleEntity == null || possibleEntity.isDead) return@forEach
			
			if (blazes.contains(possibleEntity) || hpMap.keys.contains(possibleEntity)) return@forEach
			
			hpMap[possibleEntity] = health
			blazes.add(possibleEntity)
		}
		
		blazes.sortWith(Comparator.comparingInt { hpMap[it]!! })
		
		if (blazes.size == 10 && trueTimeStarted == null) trueTimeStarted = System.currentTimeMillis()
		if (blazes.size == 9 && blazeStarted == null) blazeStarted = System.currentTimeMillis()
		
		if (blazes.isEmpty()) {
			if (lastBlazeCount != 1) return
			
			val timeTaken = (System.currentTimeMillis() - blazeStarted!!) / 10.0 / 100.0
			val trueTimeTaken = (System.currentTimeMillis() - trueTimeStarted!!) / 10.0 / 100.0
			
			val text = "&bBlaze Puzzle took &d${timeTaken}s"
			clickableChat(text, "/na copy $text", "&fTrue time taken: &b${trueTimeTaken}")
			
			lastBlazeCount = 0
			sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} Blaze Puzzle took ${timeTaken}s")
			return
		}
		
		lastBlazeCount = blazes.size
		
		val (X, Z) = getRoomCenter()
		
		if (mc.theWorld.getBlockAt(BlockPos(X + 1, 118, Z)).getBlockId() != 4) {
			blazes.reverse()
		}
	}

	
	@SubscribeEvent
	@Suppress("UNUSED_PARAMETER")
	fun renderBlazeSolve(e: RenderWorldLastEvent) {
		if (!config.BlazeSolver || !inDungeons || inBoss) return
		
		blazes.forEachIndexed { i, blaze ->
			val color = when (i) {
				0 -> config.BlazeSolverFirstBlazeColor
				1 -> config.BlazeSolverSecondBlazeColor
				else -> config.BlazeSolverThirdBlazeColor
			}
			
			drawEntityBox(blaze, color, outline = true, fill = true)
			
			if (i in 1..2) {
				val b1 = blazes[i - 1].positionVector.add(Vec3(.0, blazes[i - 1].height / 2.0, .0))
				val b2 = blazes[i].positionVector.add(Vec3(.0, blazes[i].height / 2.0, .0))
				draw3DLine(b1, b2, config.BlazeSolverLineColor, 3f)
			}
		}
	}
	
	@SubscribeEvent
	fun outlineBlazes(event: RenderLivingEntityEvent) {
		if (!inDungeons || inBoss) return
		
		blazes.forEachIndexed { index, entity ->
			if (entity.entityId != event.entity.entityId) return@forEachIndexed
			
			val color = when (index) {
				0 -> config.BlazeSolverFirstBlazeColor
				1 -> config.BlazeSolverSecondBlazeColor
				else -> config.BlazeSolverThirdBlazeColor
			}
			
			outlineESP(event, color, 5f)
		}
	}
}
