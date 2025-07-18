package noammaddons.features.impl.dungeons

import kotlinx.coroutines.launch
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.WebUtils


object BetterFloors: Feature("Basically my FunnyMapExtras's config port. Place and remove some blocks in the boss fight") {
    private val f7 = ToggleSetting("F7")
    private val f6 = ToggleSetting("F6")
    private val f5 = ToggleSetting("F5")
    override fun init() = addSettings(f7, f6, f5)

    private var f7Config: Map<String, List<Map<String, Double>>>? = null
    private var f6Config: Map<String, List<Map<String, Double>>>? = null
    private var f5Config: Map<String, List<Map<String, Double>>>? = null

    private val blockStates = mapOf(
        "Air" to Blocks.air.defaultState,

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
        "Chest" to Blocks.ender_chest.defaultState,
        "Bedrock" to Blocks.bedrock.defaultState,
        "CobbleWall" to Blocks.cobblestone_wall.defaultState,

        // Clay types
        "GrayClay" to Blocks.stained_hardened_clay.getStateFromMeta(9), // Gray Clay
    )

    init {
        WebUtils.fetchJson<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/betterFloors/F7BossCoords.json"
        ) { f7Config = it }

        WebUtils.fetchJson<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/betterFloors/F6BossCoords.json"
        ) { f6Config = it }

        WebUtils.fetchJson<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/betterFloors/F5BossCoords.json"
        ) { f5Config = it }
    }


    private fun placeBlocks(json: Map<String, List<Map<String, Double>>>) {
        scope.launch {
            for ((type, positions) in json) {
                val blockType = blockStates[type] ?: continue
                positions.forEach { coords ->
                    val position = BlockPos(coords["x"] !!, coords["y"] !!, coords["z"] !!)
                    ghostBlock(position, blockType)
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! inBoss) return
        when (dungeonFloorNumber) {
            7 -> if (f7.value) f7Config?.let(::placeBlocks)
            6 -> if (f6.value) f6Config?.let(::placeBlocks)
            5 -> if (f5.value) f5Config?.let(::placeBlocks)
        }
    }
}