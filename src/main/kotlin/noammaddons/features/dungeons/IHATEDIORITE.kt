package noammaddons.features.dungeons

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.ThreadUtils

object IHATEDIORITE: Feature() {
    private var iHateDioriteBlocks: Map<String, List<BlockPos>>? = null
    private val blockTypes = mapOf(
        "GreenArray" to Blocks.stained_glass.getStateFromMeta(5),
        "YellowArray" to Blocks.stained_glass.getStateFromMeta(4),
        "PurpleArray" to Blocks.stained_glass.getStateFromMeta(10),
        "RedArray" to Blocks.stained_glass.getStateFromMeta(14)
    )

    init {
        fetchJsonWithRetry<Map<String, List<Map<String, Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/iHateDioriteBlocks.json"
        ) { data ->
            data ?: return@fetchJsonWithRetry

            val newMap = mutableMapOf<String, List<BlockPos>>()
            data.forEach { (key, value) ->
                newMap[key] = value.map {
                    BlockPos(
                        it["x"] as Double,
                        it["y"] as Double,
                        it["z"] as Double
                    )
                }
            }
            iHateDioriteBlocks = newMap
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (! config.IHATEDIORITE) return
        if (event.phase != TickEvent.Phase.END) return
        if (iHateDioriteBlocks == null) return
        if (F7Phase != 2) return

        ThreadUtils.runOnNewThread {
            iHateDioriteBlocks !!.forEach { (key, coordsList) ->
                val blockType = blockTypes[key] ?: return@forEach
                coordsList.forEach { pos ->
                    for (i in 0 .. 37) {
                        val ghostPos = pos.add(0, i, 0)
                        if (getBlockAt(ghostPos).getBlockId() == 1) {
                            ghostBlock(ghostPos, blockType)
                        }
                    }
                }
            }
        }
    }
}



