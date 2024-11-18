package noammaddons.features.dungeons

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.Utils.isNull

object IHATEDIORITE: Feature() {
    private var iHateDioriteBlocks: Map<String, List<Map<String, Double>>>? = null
    private val blockTypes = mapOf(
        "GreenArray" to Blocks.stained_glass.getStateFromMeta(5),
        "YellowArray" to Blocks.stained_glass.getStateFromMeta(4),
        "PurpleArray" to Blocks.stained_glass.getStateFromMeta(10),
        "RedArray" to Blocks.stained_glass.getStateFromMeta(14)
    )

    init {
        fetchJsonWithRetry<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/iHateDioriteBlocks.json"
        ) { iHateDioriteBlocks = it }
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! config.IHATEDIORITE) return
        if (iHateDioriteBlocks.isNull()) return
        if (F7Phase != 5) return

        iHateDioriteBlocks !!.forEach { (key, coordsList) ->
            val blockType = blockTypes[key] ?: return@forEach

            coordsList.forEach { coord ->
                val pos = BlockPos(
                    (coord["x"] as Double),
                    (coord["y"] as Double),
                    (coord["z"] as Double)
                )

                (0 .. 37).asSequence()
                    .map { pos.add(0, it, 0) }
                    .filter { getBlockAt(it)?.getBlockId() == 1 }
                    .forEach { ghostBlock(it, blockType) }
            }
        }
    }
}



