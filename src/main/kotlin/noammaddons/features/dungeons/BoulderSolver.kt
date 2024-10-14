package noammaddons.features.dungeons

import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ScanUtils.Utils.getPuzzleRotation
import noammaddons.utils.ScanUtils.Utils.getRealCoord
import noammaddons.utils.ThreadUtils.runEvery
import noammaddons.utils.Utils.isNull
import java.awt.Color


object BoulderSolver {
	private data class SolutionsWrapper(val solutions: Map<String, List<List<Int>>>)
	private var data: SolutionsWrapper? = null
	private val gridBlocks = mutableSetOf<BlockPos>()
	private var renderBlocks = mutableListOf<BlockPos>()
	private var hasSolution = false
	private var enteredRoomAt: Long? = null
	private var puzzleDone = false
	private val relativeCoords = mapOf(
		"ironbar" to listOf(0, 70, 12),
		"chest" to listOf(0, 66, -14),
		"firstbox" to listOf(-9, 66, -9)
	)
	
	// Debugging: Render the current solution
	fun renderSolutions() {
		if (renderBlocks.isEmpty()) { return }
		renderBlocks.forEach { pos ->
			drawBlockBox(
				pos,
				Color(0, 114, 255, 85),
				outline = true,
				fill = true,
				phase = true
			)
		}
	}
	
	// Debugging: Reset the solver
	fun reset() {
		modMessage("Resetting Boulder Solver data.")
		enteredRoomAt = null
		renderBlocks.clear()
		gridBlocks.clear()
		hasSolution = false
		puzzleDone = false
	}
	
	// Debugging: Handle block placement
	fun onBlockPlacement(block: Block, pos: BlockPos) {
		modMessage("Handling block placement at $pos with block: $block")
		if (enteredRoomAt == null) {
			modMessage("No room entry timestamp found, skipping.")
			return
		}
		
		if (block == Blocks.chest) {
			enteredRoomAt?.let {
				val timeTaken = ((System.currentTimeMillis() - it) / 1000).toDouble().toFixed(2)
				modMessage("&dBoulder took&f: &6${timeTaken}s")
				sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} Boulder took: ${timeTaken}s")
			}
			puzzleDone = true
			reset()
			return
		}
		
		if (block.equalsOneOf(Blocks.wall_sign, Blocks.stone)) {
			var blocksScanned = 0
			gridBlocks.forEach { gBlock ->
				if (gBlock.compareTo(pos) in 1..2) blocksScanned++
			}
			
			modMessage("Scanned $blocksScanned blocks.")
			if (blocksScanned < 1) return
			renderBlocks.removeFirstOrNull()
		}
	}
	
	init {
		fetchJsonWithRetry<SolutionsWrapper?>(
			"https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/BoulderSolutions.json"
		) { data ->
			this.data = data
			modMessage("Successfully fetched Boulder solutions.")
		}
	}
	
	
	
	
	
	// Debugging: Generate the grid code
	fun getBoulderGrid(rotation: Int): String {
		var str = ""
		
		for (z in 0..15 step 3) {
			for (x in 0..18 step 3) {
				val block = mc.theWorld.getBlockAt(BlockPos(getRealCoord(listOf(x * 3, 66,  z * 3), rotation)))
				str += if (block == Blocks.air) "0" else "1"
			}
		}
		
		modMessage("Generated grid string: $str")
		return str
	}
	
	
	
	
	
	
	
	
	
	
	
	@SubscribeEvent
	fun onTick(event: Tick) {
		if (!inDungeons || !enteredRoomAt.isNull() || puzzleDone) {
			modMessage("Skipping runEvery due to conditions not met.")
			return
		}
		
		val roomRotation = getPuzzleRotation() ?: 0
		val block = mc.theWorld.getBlockAt(BlockPos(getRealCoord(relativeCoords["ironbar"]!!, roomRotation)))
		modMessage("Checking block at ironbar position: $block")
		
		if (block != Blocks.iron_bars) return
		
		val gridCode = getBoulderGrid(roomRotation)
		val currentSolution = data?.solutions?.get(gridCode)
		
		if (currentSolution == null) {
			modMessage("&bBoulder room variant not found in the data")
			return
		}
		
		modMessage("Found solution for grid code: $gridCode")
		hasSolution = true
		currentSolution.forEach { coord ->
			val solutionBlock = BlockPos(getRealCoord(coord, roomRotation))
			renderBlocks.add(solutionBlock)
			gridBlocks.add(solutionBlock)
		}
		
		enteredRoomAt = System.currentTimeMillis()
		modMessage("Solution found, starting timer.")
	}
	
	
	@SubscribeEvent
	fun drawSolution(event: RenderWorldLastEvent) {
		if (mc.theWorld.isNull() || !inDungeons) return
		renderSolutions()
	}
	
	@SubscribeEvent
	fun onWorldUnload(event: WorldEvent.Unload) {
		reset()
	}
	
	@SubscribeEvent
	fun onInteract(event: PlayerInteractEvent) {
		if (mc.theWorld.isNull() || !inDungeons) return
		if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return
		onBlockPlacement(mc.theWorld.getBlockAt(event.pos ?: return), event.pos ?: return)
	}
}

