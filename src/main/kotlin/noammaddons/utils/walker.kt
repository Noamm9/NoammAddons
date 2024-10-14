package noammaddons.utils

import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.BlockPos
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.PlayerUtils.Player

object walker {
	private val keybinds = mapOf(
		"forward" to mc.gameSettings.keyBindForward.keyCode,
		"left" to mc.gameSettings.keyBindLeft.keyCode,
		"right" to mc.gameSettings.keyBindRight.keyCode,
		"back" to mc.gameSettings.keyBindBack.keyCode,
		"jump" to mc.gameSettings.keyBindJump.keyCode,
		"sneak" to mc.gameSettings.keyBindSneak.keyCode
	)
	
	private val keyState = mutableMapOf<String, Boolean>().apply {
		keybinds.keys.forEach { this[it] = false }
	}
	
	private fun calcDir(x1: Int, z1: Int, x2: Int, z2: Int): Int {
		return when {
			z2 > z1 -> 0
			x2 < x1 -> 1
			z2 < z1 -> 2
			x2 > x1 -> 3
			else -> -1
		}
	}
	
	private fun calcDiff(coord1: Pair<Int, Int>, coord2: Pair<Int, Int>): Pair<Int, Int> {
		return Pair(coord2.first - coord1.first, coord2.second - coord1.second)
	}
	
	private fun add(coord1: Pair<Int, Int>, coord2: Pair<Int, Int>): Pair<Int, Int> {
		return Pair(coord1.first + coord2.first, coord1.second + coord2.second)
	}
	
	fun walk(path: MutableList<Pair<Int, Int>>): Boolean {
		keyState.keys.forEach { keyState[it] = false }
		val (x, y, z) = Triple(Player!!.posX, Player!!.posY, Player!!.posZ)
		val (mx, mz) = Pair(Player!!.motionX, Player!!.motionZ)
		val dirOffset = (((Player!!.rotationYaw + 360) % 360 + 45) / 90).toInt() % 4
		
		if (y < 70) return true
		
		while (path.size > 1) {
			val (x1, z1) = path.removeAt(0)
			if (x1 != x.toInt() || z1 != z.toInt()) continue
			
			val dir = calcDir(x1, z1, path[0].first, path[0].second)
			var turnDist = 9999
			
			for (i in 0 until path.size - 1) {
				val (nx1, nz1) = path[i]
				val (nx2, nz2) = path[i + 1]
				if (calcDir(nx1, nz1, nx2, nz2) != dir) {
					turnDist = i
					break
				}
			}
			
			keyState["sneak"] = true
			
			if (turnDist != 9999) {
				val potential = add(path[turnDist], calcDiff(Pair(x1, z1), path[0]))
				val block = mc.theWorld.getBlockState(BlockPos(potential.first, y.toInt(), potential.second)).block
				if (block.unlocalizedName == "tile.stone") keyState["sneak"] = false
			} else keyState["sneak"] = false
			
			if (turnDist > 3) keyState["sneak"] = false
			
			val movementKeys = listOf("forward", "right", "back", "left").toMutableList()
			for (i in 0 until dirOffset) movementKeys.add(0, movementKeys.removeAt(movementKeys.size - 1))
			
			var corr = -1
			when {
				mz > 0 && z > z1 + 0.55 && dir != 0 -> corr = 2
				mx < 0 && x < x1 + 0.45 && dir != 1 -> corr = 3
				mz < 0 && z < z1 + 0.45 && dir != 2 -> corr = 0
				mx > 0 && x > x1 + 0.55 && dir != 3 -> corr = 1
			}
			
			if (dir != -1) keyState[movementKeys[dir]] = true
			if (corr != -1) keyState[movementKeys[corr]] = true
			
			updateKeys()
			break
		}
		
		if (path.size <= 1) {
			keyState.keys.forEach { keyState[it] = false }
			updateKeys()
			return true
		}
		
		return false
	}
	
	fun updateKeys() {
		keyState.forEach { (key, state) ->
			KeyBinding.setKeyBindState(keybinds[key] ?: return@forEach, state)
		}
	}
}
