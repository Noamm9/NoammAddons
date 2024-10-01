package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.BlockUtils.toAir
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent


object BetterFloors {
    private var f7Config: Map<String, List<Map<String, Double>>>? = null
    private var f6Config: Map<String, List<Map<String, Double>>>? = null
    private var f5Config: Map<String, List<Map<String, Double>>>? = null
	
	private val blockStates = mapOf(
		// Glass types
		"Glass" to Blocks.glass.defaultState,
		"WhiteGlass" to Blocks.stained_glass.defaultState,
		"OrangeGlass" to Blocks.stained_glass.getStateFromMeta(1),
		"LightBlueGlass" to Blocks.stained_glass.getStateFromMeta(3),
		"YellowGlass" to Blocks.stained_glass.getStateFromMeta(4),
		"LimeGlass" to Blocks.stained_glass.getStateFromMeta(5),
		"PurpleGlass" to Blocks.stained_glass.getStateFromMeta(10),
		"BrownGlass" to Blocks.stained_glass.getStateFromMeta(12),
		"RedGlass" to Blocks.stained_glass.getStateFromMeta(14),
		
		// Wool types
		"LimeWool" to Blocks.wool.getStateFromMeta(5),
		"YellowWool" to Blocks.wool.getStateFromMeta(4),
		"PurpleWool" to Blocks.wool.getStateFromMeta(10),
		"LightBlueWool" to Blocks.wool.getStateFromMeta(3),
		"OrangeWool" to Blocks.wool.getStateFromMeta(1),
		"RedWool" to Blocks.wool.getStateFromMeta(14),
		
		// Carpet types
		"LimeCarpet" to Blocks.carpet.getStateFromMeta(5),
		"PurpleCarpet" to Blocks.carpet.getStateFromMeta(10),
		
		// Wood planks and other block types
		"BirchFence" to Blocks.birch_fence.defaultState,
		"DarkOakWoodPlank" to Blocks.planks.getStateFromMeta(5), // Dark Oak Wood Plank
		
		// Miscellaneous blocks
		"Chest" to Blocks.chest.defaultState,
		"Bedrock" to Blocks.bedrock.defaultState,
		
		// Clay types
		"GrayClay" to Blocks.stained_hardened_clay.getStateFromMeta(9) // Gray Clay
	)
	
	
    init {
        fetchJsonWithRetry<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/betterFloors/F7BossCoords.json"
        ) { f7Config = it }
	    
	    fetchJsonWithRetry<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/betterFloors/F6BossCoords.json"
        ) { f6Config = it }
	    
	    fetchJsonWithRetry<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/betterFloors/F5BossCoords.json"
        ) { f5Config = it }
    }
	

    private fun placeBlocks(json: Map<String, List<Map<String, Double>>>) {
        try {
            json.forEach { (type, positions) ->
                val blockType = blockStates[type]

                positions.forEach { coords ->
                    val position = BlockPos(
	                    coords["x"] ?: return,
	                    coords["y"] ?: return,
	                    coords["z"] ?: return
					)

                    if (type == "Air") toAir(position)
                    else blockType?.let { ghostBlock(position, it) }
                }
            }
        } catch (a: Exception) {
			println(a)
		}
    }


    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
	    if (!config.BetterFloors) return
        if (!inBoss) return
	    
        when (dungeonFloor) {
            7 -> if (config.BetterFloor7) f7Config?.let { placeBlocks(it) }
            6 -> if (config.BetterFloor6) f6Config?.let { placeBlocks(it) }
            5 -> if (config.BetterFloor5) f5Config?.let { placeBlocks(it) }
        }
    }
}