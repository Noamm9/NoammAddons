package noammaddons.features.impl.dungeons.dmap.core

import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.coordMultiplier
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils.startCorner
import noammaddons.utils.DungeonUtils.DungeonPlayer


data class DungeonMapPlayer(val teammate: DungeonPlayer, val skin: ResourceLocation) {
    var mapX = 0f
    var mapZ = 0f
    var yaw = 0f
    var icon = ""

    fun getRealPos() = BlockPos(
        (mapX - startCorner.first) / coordMultiplier + DungeonScanner.startX - 15,
        teammate.entity?.posY ?: .0,
        (mapZ - startCorner.second) / coordMultiplier + DungeonScanner.startZ - 15
    )
}
