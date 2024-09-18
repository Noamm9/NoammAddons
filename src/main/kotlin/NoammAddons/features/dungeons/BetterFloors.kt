package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.config.PogObject
import NoammAddons.utils.BlockUtils.ghostBlock
import NoammAddons.utils.BlockUtils.toAir
import NoammAddons.utils.LocationUtils.dungeonFloor
import NoammAddons.utils.LocationUtils.inBoss
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent


object BetterFloors {
    private val f7Config get() = PogObject.JsonHelper.readJsonFile("$MOD_ID:betterFloors/F7BossCoords.json")
    private val f6Config get() = PogObject.JsonHelper.readJsonFile("$MOD_ID:betterFloors/F6BossCoords.json")
    private val f5Config get() = PogObject.JsonHelper.readJsonFile("$MOD_ID:betterFloors/F5BossCoords.json")
    private var tickTimer = 0


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

    private fun placeBlocks(json: JsonObject) {
        try {
            json.entrySet().forEach { (type, value) ->
                val blockType = blockStates[type]
               // if (config.DevMode) println("type: $type, value: $value")

                if (value !is JsonArray) return@forEach

                value.filterIsInstance<JsonObject>().forEach { element ->
                    val x = element["x"]?.asInt ?: 0
                    val y = element["y"]?.asInt ?: 0
                    val z = element["z"]?.asInt ?: 0
                    val position = BlockPos(x, y, z)


                    if (type == "Air") toAir(position)
                    else blockType?.let { ghostBlock(position, it) }
                }
            }
        }
        catch (e: Exception) {
            println("Error: $e")
        }
    }

    @SubscribeEvent
    fun runEverySec(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        tickTimer++
        if (tickTimer % 20 != 0) return
        if (!inBoss) return

        when (dungeonFloor) {
            7 -> if (config.BetterFloor7) f7Config?.let { placeBlocks(it) }
            6 -> if (config.BetterFloor6) f6Config?.let { placeBlocks(it) }
            5 -> if (config.BetterFloor5) f5Config?.let { placeBlocks(it) }
            else -> return
        }
    }
}